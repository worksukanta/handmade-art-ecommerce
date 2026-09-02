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

import java.time.OffsetDateTime;

/**
 * Service for Quotation operations (Phase 3E.1 + 3E.2 scope).
 *
 * Admin endpoints (REST API Spec §14):
 *   POST /api/v1/admin/custom-requests/{id}/quotation — createQuotation
 *   GET  /api/v1/admin/quotations/{id}                — adminGetQuotation
 *
 * Customer endpoints (REST API Spec §14):
 *   GET  /api/v1/custom-requests/{id}/quotation       — customerGetQuotation
 *   POST /api/v1/quotations/{id}/approve              — approveQuotation (Phase 3E.2)
 *   POST /api/v1/quotations/{id}/reject               — rejectQuotation (Phase 3E.2)
 *
 * Quotation creation rules:
 *   - Request must be UNDER_REVIEW (valid pre-state).
 *   - One quotation per request — duplicate attempt → 409 DUPLICATE_QUOTATION (DEC-004 DEFERRED).
 *   - quotedAmount: BigDecimal, >= 0 (validated by DTO + DB CHECK).
 *   - advanceAmount: optional absolute value — DEC-005 APPROVED; no fixed percentage.
 *   - expiryAt: must be a future datetime.
 *   - After creation: request status transitions UNDER_REVIEW → QUOTED.
 *   - Quotation status set to PENDING on creation.
 *
 * Quotation approval rules (DEC-005 APPROVED):
 *   - Customer may only act on quotations belonging to their own request.
 *   - Only quotations in PENDING status can be approved or rejected.
 *   - Expired quotations (expiryAt in the past) cannot be approved (BR-06, FR-CUST-10).
 *   - Approval: quotation PENDING → APPROVED; request QUOTED → APPROVED (via CUSTOMER_APPROVAL_PENDING).
 *   - Rejection: quotation PENDING → REJECTED; request QUOTED → REJECTED (terminal).
 *   - Customer cannot alter quotation amount or advance amount.
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

    // =========================================================================
    // POST /api/v1/quotations/{id}/approve — Customer approves quotation
    // =========================================================================

    /**
     * Customer approves the quotation identified by quotation ID.
     *
     * Ownership rule: the quotation must belong to a custom request owned by
     * the authenticated customer (non-disclosure: foreign/missing → 404).
     *
     * Pre-conditions (DEC-005 APPROVED, BR-06, FR-CUST-10):
     *   - Quotation must exist and belong to the customer's own request.
     *   - Quotation status must be PENDING (only valid approvable state).
     *   - Quotation must not be expired (expiryAt must be in the future).
     *   - Request must be in QUOTED state (valid approval-eligible state).
     *
     * Post-conditions (atomic transaction):
     *   - Quotation status: PENDING → APPROVED; decidedAt set to now.
     *   - Request status: QUOTED → APPROVED.
     *   - Customer cannot supply or alter quotation/advance amounts.
     *
     * @param currentUser  authenticated customer
     * @param quotationId  path variable — the quotation's primary key
     * @return QuotationResponse with updated status
     */
    @Transactional
    public QuotationResponse approveQuotation(AppUser currentUser, Long quotationId) {

        Quotation quotation = resolveOwnedQuotation(currentUser, quotationId);

        // Only PENDING quotations can be approved
        if (quotation.getStatus() != QuotationStatus.PENDING) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot approve quotation: status is " + quotation.getStatus() +
                    " (must be PENDING)");
        }

        // Expiry check (BR-06, FR-CUST-10) — expired quotations cannot be approved
        if (!quotation.getExpiryAt().isAfter(OffsetDateTime.now())) {
            // Mark quotation as EXPIRED and request as QUOTATION_EXPIRED
            quotation.setStatus(QuotationStatus.EXPIRED);
            quotationRepository.save(quotation);
            CustomOrderRequest req = quotation.getCustomOrderRequest();
            req.setStatus(CustomOrderRequestStatus.QUOTATION_EXPIRED);
            requestRepository.save(req);
            throw new InvalidWorkflowTransitionException(
                    "Cannot approve quotation: quotation has expired");
        }

        // Request must be in QUOTED state for approval to be valid
        CustomOrderRequest req = quotation.getCustomOrderRequest();
        if (req.getStatus() != CustomOrderRequestStatus.QUOTED) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot approve quotation: custom request is in state " + req.getStatus() +
                    " (must be QUOTED)");
        }

        // Transition: quotation PENDING → APPROVED
        quotation.setStatus(QuotationStatus.APPROVED);
        quotation.setDecidedAt(OffsetDateTime.now());
        quotationRepository.save(quotation);

        // Transition: request QUOTED → APPROVED (approved lifecycle — ERD §15.3)
        req.setStatus(CustomOrderRequestStatus.APPROVED);
        requestRepository.save(req);

        return QuotationResponse.from(quotation);
    }

    // =========================================================================
    // POST /api/v1/quotations/{id}/reject — Customer rejects quotation
    // =========================================================================

    /**
     * Customer rejects the quotation identified by quotation ID.
     *
     * Ownership rule: the quotation must belong to a custom request owned by
     * the authenticated customer (non-disclosure: foreign/missing → 404).
     *
     * Pre-conditions:
     *   - Quotation must exist and belong to the customer's own request.
     *   - Quotation status must be PENDING (only valid rejectable state).
     *   - Request must be in QUOTED state.
     *
     * Post-conditions (atomic transaction):
     *   - Quotation status: PENDING → REJECTED; decidedAt set to now.
     *   - Request status: QUOTED → REJECTED (terminal state).
     *
     * @param currentUser  authenticated customer
     * @param quotationId  path variable — the quotation's primary key
     * @return QuotationResponse with updated status
     */
    @Transactional
    public QuotationResponse rejectQuotation(AppUser currentUser, Long quotationId) {

        Quotation quotation = resolveOwnedQuotation(currentUser, quotationId);

        // Only PENDING quotations can be rejected
        if (quotation.getStatus() != QuotationStatus.PENDING) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot reject quotation: status is " + quotation.getStatus() +
                    " (must be PENDING)");
        }

        // Request must be in QUOTED state for rejection to be valid
        CustomOrderRequest req = quotation.getCustomOrderRequest();
        if (req.getStatus() != CustomOrderRequestStatus.QUOTED) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot reject quotation: custom request is in state " + req.getStatus() +
                    " (must be QUOTED)");
        }

        // Transition: quotation PENDING → REJECTED
        quotation.setStatus(QuotationStatus.REJECTED);
        quotation.setDecidedAt(OffsetDateTime.now());
        quotationRepository.save(quotation);

        // Transition: request QUOTED → REJECTED (terminal state — ERD §15.3)
        req.setStatus(CustomOrderRequestStatus.REJECTED);
        requestRepository.save(req);

        return QuotationResponse.from(quotation);
    }

    // =========================================================================
    // Private helper — ownership-verified quotation resolution
    // =========================================================================

    /**
     * Resolve a quotation by ID and verify it belongs to the authenticated customer's request.
     * Non-disclosure: returns 404 for missing or foreign quotations.
     */
    private Quotation resolveOwnedQuotation(AppUser currentUser, Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        CustomOrderRequest req = quotation.getCustomOrderRequest();
        if (!req.getUser().getId().equals(currentUser.getId())) {
            // Non-disclosure: foreign quotation appears as 404
            throw new ResourceNotFoundException("Quotation not found");
        }
        return quotation;
    }
}
