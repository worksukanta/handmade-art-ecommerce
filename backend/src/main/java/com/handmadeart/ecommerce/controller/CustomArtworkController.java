package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomOrderImageResponse;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.CustomArtworkRequestService;
import com.handmadeart.ecommerce.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Customer-facing custom artwork request controller.
 *
 * Endpoints (REST API Spec §13, §14 customer read):
 *   POST  /api/v1/custom-requests              — submit a custom artwork request
 *   GET   /api/v1/custom-requests              — list authenticated customer's requests
 *   GET   /api/v1/custom-requests/{id}         — get detail of an owned request
 *   POST  /api/v1/custom-requests/{id}/images  — upload a reference image for an owned request
 *   GET   /api/v1/custom-requests/{id}/quotation — view quotation for an owned request
 *
 * Authorization:
 *   CUSTOMER role required (enforced by SecurityConfig: /api/v1/custom-requests/**).
 *   Identity resolved from JWT via CurrentUserService.
 *   No client-supplied user IDs are trusted.
 *
 * Ownership enforcement:
 *   All customer operations are scoped to the authenticated user via CurrentUserService.
 *   A foreign or missing requestId returns 404 (non-disclosure semantics).
 */
@RestController
@RequestMapping("/api/v1/custom-requests")
public class CustomArtworkController {

    private final CustomArtworkRequestService customArtworkRequestService;
    private final QuotationService quotationService;
    private final CurrentUserService currentUserService;

    public CustomArtworkController(CustomArtworkRequestService customArtworkRequestService,
                                    QuotationService quotationService,
                                    CurrentUserService currentUserService) {
        this.customArtworkRequestService = customArtworkRequestService;
        this.quotationService = quotationService;
        this.currentUserService = currentUserService;
    }

    /**
     * Submit a new custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/custom-requests
     * Auth:    CUSTOMER
     * Request: CustomArtworkRequestCreateRequest (validated)
     * Success: 201 Created + CustomArtworkRequestResponse
     * Errors:  400 validation, 401, 403
     */
    @PostMapping
    public ResponseEntity<CustomArtworkRequestResponse> createCustomRequest(
            @Valid @RequestBody CustomArtworkRequestCreateRequest request) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomArtworkRequestResponse response =
                customArtworkRequestService.createCustomRequest(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all custom requests for the authenticated customer.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests
     * Auth:    CUSTOMER
     * Params:  page (default 0), size (default 20), status (optional filter)
     * Success: 200 OK + PageResponse&lt;CustomArtworkRequestSummary&gt;
     * Errors:  401, 403
     */
    @GetMapping
    public ResponseEntity<PageResponse<CustomArtworkRequestSummary>> listCustomRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CustomOrderRequestStatus status) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        PageResponse<CustomArtworkRequestSummary> response =
                customArtworkRequestService.listCustomRequests(currentUser, page, size, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detail of a single custom artwork request owned by the authenticated customer.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}
     * Auth:    CUSTOMER
     * Success: 200 OK + CustomArtworkRequestResponse
     * Errors:  401, 403, 404 (foreign or missing id)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomArtworkRequestResponse> getCustomRequest(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomArtworkRequestResponse response =
                customArtworkRequestService.getCustomRequest(currentUser, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload a reference image for an owned custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/custom-requests/{id}/images
     * Auth:    CUSTOMER
     * Request: multipart/form-data; field name "file"
     * Success: 201 Created + CustomOrderImageResponse
     * Errors:  400 invalid type/empty, 401, 403, 404, 413 size (DEC-003 OPEN)
     *
     * DEC-003 OPEN: size limit not enforced; only content-type image/* validated.
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<CustomOrderImageResponse> uploadReferenceImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomOrderImageResponse response =
                customArtworkRequestService.uploadReferenceImage(currentUser, id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * View the quotation for an owned custom artwork request.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}/quotation
     * Auth:    CUSTOMER
     * Success: 200 OK + QuotationResponse
     * Errors:  401, 403, 404 (foreign/missing request or no quotation yet)
     */
    @GetMapping("/{id}/quotation")
    public ResponseEntity<QuotationResponse> getQuotation(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        QuotationResponse response = quotationService.customerGetQuotation(currentUser, id);
        return ResponseEntity.ok(response);
    }
}
