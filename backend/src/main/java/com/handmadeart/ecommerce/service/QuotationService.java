package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.customartwork.QuotationCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.exception.DuplicateQuotationException;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Quotation operations (Phase 3E.1 scope).
 *
 * Admin endpoints (REST API Spec §14):
 *   POST /api/v1/admin/custom-requests/{id}/quotation — createQuotation
 *   GET  /api/v1/admin/quotations/{id}                — adminGetQuotation
 *
 * Customer endpoint (REST API Spec §14):
 *   GET  /api/v1/custom-requests/{id}/quotation       — customerGetQuotation
 *
 * Out of scope for Phase 3E.1 (not implemented here):
 *   POST /api/v1/quotations/{id}/approve — quotation approval
 *   POST /api/v1/quotations/{id}/reject  — quotation rejection
 *
 * Quotation creation rules:
 *   - Request must be UNDER_REVIEW (valid pre-state).
 *   - One quotation per request — duplicate attempt → 409 DUPLICATE_QUOTATION (DEC-004 DEFERRED).
 *   - quotedAmount: BigDecimal, >= 0 (validated by DTO + DB CHECK).
 *   - advanceAmount: optional absolute value — DEC-005 OPEN; no fixed percentage.
 *   - expiryAt: must be a future datetime.
 *   - After creation: request status transitions UNDER_REVIEW → QUOTED.
 *   - Quotation status set to PENDING on creation.
 *
 * Customer quotation read:
 *   - Customer may only view the quotation for their own request (ownership via requestId).
 *   - Admin quotation read: direct by quotation ID.
 */
@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomOrderRequestRepository requestRepository;

    public QuotationService(QuotationRepository quotationRepository,
                            CustomOrderRequestRepository requestRepository) {
        this.quotationRepository = quotationRepository;
        this.requestRepository = requestRepository;
    }

    // =========================================================================
    // POST /api/v1/admin/custom-requests/{id}/quotation — Admin creates quotation
    // =========================================================================

    /**
     * Create a quotation for a custom artwork request.
     *
     * Pre-conditions:
     *   - Request must exist (404 otherwise).
     *   - Request must be UNDER_REVIEW → InvalidWorkflowTransitionException (409) otherwise.
     *   - No quotation may already exist for this request (DEC-004 DEFERRED) → 409 otherwise.
     *   - expiryAt must be in the future → 400 otherwise.
     *
     * Post-conditions:
     *   - Quotation persisted with status PENDING.
     *   - Request status transitions UNDER_REVIEW → QUOTED.
     *   - Both changes are within the same transaction.
     *
     * DEC-005 OPEN: advanceAmount is an Admin-entered absolute value; no fixed percentage.
     *
     * @param adminUser   authenticated Admin (createdBy)
     * @param requestId   path variable — the custom request to quote
     * @param createReq   quotation data
     * @return QuotationResponse for the created quotation
     */
    @Transactional
    public QuotationResponse createQuotation(AppUser adminUser, Long requestId,
                                              QuotationCreateRequest createReq) {

        // Step 1: Resolve the custom request
        CustomOrderRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        // Step 2: Validate workflow state
        if (req.getStatus() != CustomOrderRequestStatus.UNDER_REVIEW) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot create quotation: request is in state " + req.getStatus() +
                    " (must be UNDER_REVIEW)");
        }

        // Step 3: Enforce one-quotation-per-request constraint (DEC-004 DEFERRED)
        if (quotationRepository.findByCustomOrderRequestId(requestId).isPresent()) {
            throw new DuplicateQuotationException(
                    "A quotation already exists for custom request " + requestId);
        }

        // Step 4: Validate expiryAt is in the future
        if (!createReq.getExpiryAt().isAfter(java.time.OffsetDateTime.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }

        // Step 5: Create the quotation
        Quotation quotation = new Quotation();
        quotation.setCustomOrderRequest(req);
        quotation.setQuotedAmount(createReq.getQuotedAmount());
        quotation.setAdvanceAmount(createReq.getAdvanceAmount());     // nullable; DEC-005 OPEN
        quotation.setEstimatedDeliveryDate(createReq.getEstimatedDeliveryDate());
        quotation.setExpiryAt(createReq.getExpiryAt());
        quotation.setNotesTerms(createReq.getNotesTerms());
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setCreatedBy(adminUser);

        Quotation saved = quotationRepository.save(quotation);

        // Step 6: Transition request UNDER_REVIEW → QUOTED
        req.setStatus(CustomOrderRequestStatus.QUOTED);
        requestRepository.save(req);

        return QuotationResponse.from(saved);
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id}/quotation — Customer views own quotation
    // =========================================================================

    /**
     * Retrieve the quotation for a custom request owned by the authenticated customer.
     *
     * Ownership:
     *   - Request must belong to the authenticated customer (non-disclosure: 404).
     *   - No quotation → 404.
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable (the custom request ID, not the quotation ID)
     * @return QuotationResponse
     */
    @Transactional(readOnly = true)
    public QuotationResponse customerGetQuotation(AppUser currentUser, Long requestId) {

        // Verify request exists and is owned by the customer
        CustomOrderRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        if (!req.getUser().getId().equals(currentUser.getId())) {
            // Non-disclosure: foreign request appears as 404
            throw new ResourceNotFoundException("Custom request not found");
        }

        return quotationRepository.findByCustomOrderRequestId(requestId)
                .map(QuotationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quotation not found for custom request " + requestId));
    }

    // =========================================================================
    // GET /api/v1/admin/quotations/{id} — Admin views quotation by quotation ID
    // =========================================================================

    /**
     * Retrieve a quotation directly by quotation ID (Admin only).
     *
     * @param quotationId path variable — the quotation's primary key
     * @return QuotationResponse
     * @throws ResourceNotFoundException if quotation not found
     */
    @Transactional(readOnly = true)
    public QuotationResponse adminGetQuotation(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));
        return QuotationResponse.from(quotation);
    }
}
