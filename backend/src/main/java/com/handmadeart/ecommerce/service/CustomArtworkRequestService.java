package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomOrderImageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomRequestReviewRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Custom Artwork Request operations (Phase 3E.1).
 *
 * Customer endpoints (REST API Spec §13):
 *   POST  /api/v1/custom-requests              — createCustomRequest
 *   GET   /api/v1/custom-requests              — listCustomRequests
 *   GET   /api/v1/custom-requests/{id}         — getCustomRequest
 *   POST  /api/v1/custom-requests/{id}/images  — uploadReferenceImage
 *
 * Admin endpoints (REST API Spec §13):
 *   GET   /api/v1/admin/custom-requests        — adminListCustomRequests
 *   PATCH /api/v1/admin/custom-requests/{id}/review — adminReviewCustomRequest
 *
 * Ownership rules:
 *   - Customer user ID is resolved exclusively from the JWT via CurrentUserService.
 *   - Customer access to another customer's request returns 404 (non-disclosure).
 *   - Admin access is role-enforced by SecurityConfig (/api/v1/admin/**).
 *
 * Workflow transitions (service layer — Database Design &amp; ERD §12.3):
 *   REQUESTED → UNDER_REVIEW  (Admin ACCEPT review)
 *   UNDER_REVIEW → REJECTED   (Admin REJECT review)
 *   UNDER_REVIEW → QUOTED     (Admin creates quotation — performed in QuotationService)
 *
 * DEC-003 OPEN: upload size limit not enforced (image/* content-type validation only).
 */
@Service
public class CustomArtworkRequestService {

    private static final Logger log = LoggerFactory.getLogger(CustomArtworkRequestService.class);
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    private final CustomOrderRequestRepository requestRepository;
    private final CustomOrderImageRepository imageRepository;
    private final Path uploadRoot;

    public CustomArtworkRequestService(
            CustomOrderRequestRepository requestRepository,
            CustomOrderImageRepository imageRepository,
            @Value("${app.upload.reference-images:uploads/reference-images}") String uploadDir) {
        this.requestRepository = requestRepository;
        this.imageRepository = imageRepository;
        this.uploadRoot = Paths.get(uploadDir);
    }

    // =========================================================================
    // POST /api/v1/custom-requests — Customer creates a custom request
    // =========================================================================

    /**
     * Create a new custom artwork request owned by the authenticated customer.
     *
     * Initial status is REQUESTED (approved lifecycle value — Database Design §15.3).
     * Customer cannot choose arbitrary status.
     *
     * @param currentUser authenticated customer (from JWT — never client-supplied)
     * @param request     validated create request DTO
     * @return CustomArtworkRequestResponse for the persisted request
     */
    @Transactional
    public CustomArtworkRequestResponse createCustomRequest(AppUser currentUser,
                                                             CustomArtworkRequestCreateRequest request) {
        CustomOrderRequest entity = new CustomOrderRequest();
        entity.setUser(currentUser);
        entity.setProductType(request.getProductType());
        entity.setDescription(request.getDescription());
        entity.setDesignTheme(request.getDesignTheme());
        entity.setPreferredColors(request.getPreferredColors());
        entity.setDimensionsSize(request.getDimensionsSize());
        entity.setBudgetRange(request.getBudgetRange());
        entity.setRequiredDeliveryDate(request.getRequiredDeliveryDate());
        entity.setAdditionalInstructions(request.getAdditionalInstructions());

        // Initial status is always REQUESTED — customer cannot override this
        entity.setStatus(CustomOrderRequestStatus.REQUESTED);

        CustomOrderRequest saved = requestRepository.save(entity);
        return toFullResponse(saved);
    }

    // =========================================================================
    // GET /api/v1/custom-requests — Customer lists own requests
    // =========================================================================

    /**
     * List custom requests owned by the authenticated customer. Paginated.
     *
     * Ownership is enforced: only the authenticated customer's requests are returned.
     *
     * @param currentUser authenticated customer
     * @param page        page index (0-based)
     * @param size        page size
     * @param status      optional status filter (null = all statuses)
     * @return paginated summary of the customer's custom requests
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomArtworkRequestSummary> listCustomRequests(
            AppUser currentUser, int page, int size, CustomOrderRequestStatus status) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomOrderRequest> pageResult;
        if (status != null) {
            // Filter by specific status — returns as a page from the list
            List<CustomOrderRequest> filtered = requestRepository
                    .findByUserIdAndStatus(currentUser.getId(), status);
            // Wrap in a Page — convert list to page manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<CustomOrderRequest> pageContent = start >= filtered.size()
                    ? List.of() : filtered.subList(start, end);
            pageResult = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, filtered.size());
        } else {
            pageResult = requestRepository.findByUserId(currentUser.getId(), pageable);
        }

        return PageResponse.from(pageResult.map(CustomArtworkRequestSummary::from));
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id} — Customer gets own request detail
    // =========================================================================

    /**
     * Get a single custom request owned by the authenticated customer.
     *
     * Non-disclosure: a foreign or missing requestId returns 404
     * (must not reveal whether a request exists for another customer).
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable
     * @return full response DTO
     * @throws ResourceNotFoundException if request not found or not owned by customer
     */
    @Transactional(readOnly = true)
    public CustomArtworkRequestResponse getCustomRequest(AppUser currentUser, Long requestId) {
        CustomOrderRequest req = resolveOwnedRequest(currentUser, requestId);
        return toFullResponse(req);
    }

    // =========================================================================
    // POST /api/v1/custom-requests/{id}/images — Customer uploads reference image
    // =========================================================================

    /**
     * Upload a reference image for an owned custom artwork request.
     *
     * Storage architecture:
     *   - Consistent with product image upload (AdminCatalogueService).
     *   - UUID-based server-generated filename; original filename stored for display only.
     *   - Binary stored on filesystem; only metadata in PostgreSQL.
     *   - Never exposes arbitrary filesystem paths.
     *   - content-type must start with "image/" (DEC-003 OPEN — no size limit enforced).
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable; ownership verified
     * @param file        multipart file
     * @return CustomOrderImageResponse for the persisted metadata record
     * @throws ResourceNotFoundException if request not found or not owned
     * @throws IllegalArgumentException  if file is empty or content type is not image/*
     */
    @Transactional
    public CustomOrderImageResponse uploadReferenceImage(AppUser currentUser,
                                                          Long requestId,
                                                          MultipartFile file) {
        CustomOrderRequest req = resolveOwnedRequest(currentUser, requestId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Only image files are accepted (content type must start with 'image/')");
        }

        // DEC-003 OPEN: no size limit enforced here
        String extension = extensionForContentType(contentType);
        String serverFilename = UUID.randomUUID() + extension;
        Path requestDir = uploadRoot.resolve("request-" + requestId);

        try {
            Files.createDirectories(requestDir);
            Path destination = requestDir.resolve(serverFilename);
            file.transferTo(destination.toFile());
        } catch (IOException ex) {
            log.error("Failed to store reference image for request {}", requestId, ex);
            throw new RuntimeException("Image storage failed");
        }

        // Logical path — never expose the raw upload root
        String storageReference = "request-" + requestId + "/" + serverFilename;

        CustomOrderImage image = new CustomOrderImage();
        image.setCustomOrderRequest(req);
        image.setStorageReference(storageReference);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setContentType(contentType);
        image.setFileSizeBytes(Math.toIntExact(file.getSize()));

        CustomOrderImage saved = imageRepository.save(image);
        return CustomOrderImageResponse.from(saved);
    }

    // =========================================================================
    // GET /api/v1/admin/custom-requests — Admin lists all requests
    // =========================================================================

    /**
     * Admin: list all custom requests with optional status filter. Paginated.
     *
     * @param status  optional status filter
     * @param page    page index (0-based)
     * @param size    page size
     * @return paginated summary of all requests
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomArtworkRequestSummary> adminListCustomRequests(
            CustomOrderRequestStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomOrderRequest> pageResult;
        if (status != null) {
            List<CustomOrderRequest> filtered = requestRepository.findByStatus(status);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<CustomOrderRequest> pageContent = start >= filtered.size()
                    ? List.of() : filtered.subList(start, end);
            pageResult = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, filtered.size());
        } else {
            pageResult = requestRepository.findAll(pageable);
        }

        return PageResponse.from(pageResult.map(CustomArtworkRequestSummary::from));
    }

    // =========================================================================
    // PATCH /api/v1/admin/custom-requests/{id}/review — Admin reviews request
    // =========================================================================

    /**
     * Admin: record a review decision on a custom artwork request.
     *
     * Approved decisions and valid transitions:
     *   ACCEPT:
     *     REQUESTED    → UNDER_REVIEW
     *     (UNDER_REVIEW is also acceptable as a no-op re-acknowledge: stays UNDER_REVIEW)
     *   REJECT:
     *     UNDER_REVIEW → REJECTED (terminal)
     *     REQUESTED    → REJECTED (terminal — Admin can reject before taking it under review)
     *
     * Invalid transitions throw {@link InvalidWorkflowTransitionException} → 409.
     * Admin user is recorded on the request as reviewedBy.
     *
     * @param adminUser     authenticated Admin (from JWT)
     * @param requestId     path variable
     * @param reviewRequest review decision DTO
     * @return updated CustomArtworkRequestResponse
     */
    @Transactional
    public CustomArtworkRequestResponse adminReviewCustomRequest(AppUser adminUser,
                                                                  Long requestId,
                                                                  CustomRequestReviewRequest reviewRequest) {
        CustomOrderRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        String decision = reviewRequest.getDecision();
        CustomOrderRequestStatus current = req.getStatus();

        if ("ACCEPT".equals(decision)) {
            // Valid: REQUESTED → UNDER_REVIEW
            //        UNDER_REVIEW → UNDER_REVIEW (re-acknowledge; Admin may update notes)
            if (current == CustomOrderRequestStatus.REQUESTED) {
                req.setStatus(CustomOrderRequestStatus.UNDER_REVIEW);
            } else if (current == CustomOrderRequestStatus.UNDER_REVIEW) {
                // Already under review — notes may be updated; status unchanged
            } else {
                throw new InvalidWorkflowTransitionException(
                        "Cannot accept review: request is in state " + current +
                        " (must be REQUESTED or UNDER_REVIEW)");
            }
        } else if ("REJECT".equals(decision)) {
            // Valid: REQUESTED → REJECTED
            //        UNDER_REVIEW → REJECTED
            if (current == CustomOrderRequestStatus.REQUESTED
                    || current == CustomOrderRequestStatus.UNDER_REVIEW) {
                req.setStatus(CustomOrderRequestStatus.REJECTED);
            } else {
                throw new InvalidWorkflowTransitionException(
                        "Cannot reject review: request is in state " + current +
                        " (must be REQUESTED or UNDER_REVIEW)");
            }
        } else {
            // Should not happen — @Pattern on DTO prevents other values
            throw new InvalidWorkflowTransitionException("Unknown review decision: " + decision);
        }

        req.setReviewedBy(adminUser);
        if (reviewRequest.getNotes() != null) {
            req.setReviewNotes(reviewRequest.getNotes());
        }

        CustomOrderRequest saved = requestRepository.save(req);
        return toFullResponse(saved);
    }

    // =========================================================================
    // Package-scoped: resolve request by ID (used by QuotationService)
    // =========================================================================

    /**
     * Resolve a custom request by ID — used internally and by QuotationService.
     * Returns 404 if not found.
     */
    @Transactional(readOnly = true)
    public CustomOrderRequest resolveRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
    }

    /**
     * Save a custom request entity after external modification (e.g., from QuotationService).
     */
    @Transactional
    public void saveRequest(CustomOrderRequest req) {
        requestRepository.save(req);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolve and ownership-verify a custom request for the given customer.
     * Non-disclosure: returns 404 for both missing and foreign requests.
     */
    private CustomOrderRequest resolveOwnedRequest(AppUser currentUser, Long requestId) {
        CustomOrderRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        if (!req.getUser().getId().equals(currentUser.getId())) {
            // Non-disclosure: foreign request appears as 404, not 403
            throw new ResourceNotFoundException("Custom request not found");
        }
        return req;
    }

    /**
     * Build a full response DTO including reference images.
     */
    private CustomArtworkRequestResponse toFullResponse(CustomOrderRequest req) {
        List<CustomOrderImageResponse> images = imageRepository
                .findByCustomOrderRequestId(req.getId())
                .stream()
                .map(CustomOrderImageResponse::from)
                .collect(Collectors.toList());
        return CustomArtworkRequestResponse.from(req, images);
    }

    /**
     * Derive a safe file extension from a validated content type.
     * Returns an empty string for unrecognised subtypes.
     */
    private static String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png"               -> ".png";
            case "image/gif"               -> ".gif";
            case "image/webp"              -> ".webp";
            default                        -> "";
        };
    }
}
