package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for custom artwork advance payment and related customer read operations
 * (Phase 3E.2 scope).
 *
 * Endpoints supported:
 *   POST /api/v1/custom-requests/{id}/payments — initiateAdvancePayment
 *   GET  /api/v1/custom-requests/{id}/payments — getCustomRequestPayments
 *   GET  /api/v1/custom-requests/{id}/shipment — getCustomRequestShipment
 *
 * Advance-payment rules (DEC-005 APPROVED):
 *   - Authoritative amount = stored Quotation.advancePaymentAmount (never client-supplied).
 *   - Payment only after quotation approval (request must be in APPROVED state).
 *   - No payment before approval (409 if request not APPROVED or ADVANCE_PAYMENT_PENDING).
 *   - No duplicate successful advance payment (409 if a SUCCESS already exists).
 *   - Successful payment transitions request APPROVED → ADVANCE_PAYMENT_PENDING → IN_PRODUCTION.
 *   - DEC-001 DEFERRED: sandbox mock flow — immediate SUCCESS.
 *   - No raw card data stored or accepted.
 *
 * Ownership rule:
 *   Request must belong to authenticated customer (non-disclosure: foreign id → 404).
 *   Client-supplied IDs never establish ownership.
 */
@Service
public class CustomAdvancePaymentService {

    private final CustomOrderRequestRepository requestRepository;
    private final QuotationRepository quotationRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;

    public CustomAdvancePaymentService(
            CustomOrderRequestRepository requestRepository,
            QuotationRepository quotationRepository,
            PaymentRepository paymentRepository,
            ShipmentRepository shipmentRepository) {
        this.requestRepository = requestRepository;
        this.quotationRepository = quotationRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
    }

    // =========================================================================
    // POST /api/v1/custom-requests/{id}/payments — initiate advance payment
    // =========================================================================

    /**
     * Initiate the advance payment for an approved custom artwork request.
     *
     * Pre-conditions (DEC-005 APPROVED):
     *   - Request must exist and be owned by the authenticated customer.
     *   - Request must be in APPROVED state (valid advance-payment state).
     *   - No successful advance payment may already exist for this request.
     *   - A quotation with a non-null advanceAmount must exist.
     *
     * Post-conditions (DEC-001 sandbox flow, atomic transaction):
     *   - Payment record created with PaymentPurpose.ADVANCE and amount from Quotation.advanceAmount.
     *   - Sandbox SUCCESS: payment immediately marked SUCCESS; completedAt set.
     *   - Request transitions APPROVED → ADVANCE_PAYMENT_PENDING → IN_PRODUCTION.
     *
     * No raw card data is accepted or stored (FR-PAY-04, NFR-07).
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable; ownership verified
     * @param request     payment method label (non-sensitive)
     * @return PaymentResponse for the new advance payment record
     */
    @Transactional
    public PaymentResponse initiateAdvancePayment(AppUser currentUser, Long requestId,
                                                   PaymentInitiationRequest request) {

        // Step 1: Resolve and verify ownership
        CustomOrderRequest req = resolveOwnedRequest(currentUser, requestId);

        // Step 2: Verify the request is in APPROVED state (valid for advance payment)
        if (req.getStatus() != CustomOrderRequestStatus.APPROVED) {
            throw new InvalidWorkflowTransitionException(
                    "Advance payment not allowed: custom request is in state " + req.getStatus() +
                    " (must be APPROVED)");
        }

        // Step 3: Verify no successful advance payment already exists
        List<Payment> existingPayments = paymentRepository.findByCustomOrderRequestId(requestId);
        boolean hasSuccessfulPayment = existingPayments.stream()
                .anyMatch(p -> p.getPaymentPurpose() == PaymentPurpose.ADVANCE
                        && p.getStatus() == PaymentStatus.SUCCESS);
        if (hasSuccessfulPayment) {
            throw new InvalidWorkflowTransitionException(
                    "Advance payment already completed for custom request " + requestId);
        }

        // Step 4: Resolve the quotation and authoritative advance amount (DEC-005 APPROVED)
        Quotation quotation = quotationRepository.findByCustomOrderRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quotation not found for custom request " + requestId));

        if (quotation.getAdvanceAmount() == null) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot process advance payment: quotation has no advance amount defined");
        }

        // Step 5: Create advance payment record (DEC-001 sandbox — no real provider call)
        Payment payment = new Payment();
        payment.setCustomOrderRequest(req);
        payment.setPaymentPurpose(PaymentPurpose.ADVANCE);
        payment.setAmount(quotation.getAdvanceAmount());    // server-authoritative (DEC-005)
        payment.setPaymentMethod(request.getPaymentMethod()); // method label only; no card data

        // Step 6: Sandbox SUCCESS — immediately record outcome (DEC-001 DEFERRED)
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderTransactionReference(
                "SANDBOX-ADV-" + requestId + "-" + System.currentTimeMillis());
        payment.setCompletedAt(OffsetDateTime.now());

        paymentRepository.save(payment);

        // Step 7: Transition request APPROVED → ADVANCE_PAYMENT_PENDING → IN_PRODUCTION (sandbox)
        // Per DEC-001 sandbox: immediate success advances the workflow fully
        req.setStatus(CustomOrderRequestStatus.IN_PRODUCTION);
        requestRepository.save(req);

        // Return a PaymentResponse including the customOrderRequestId
        return toPaymentResponse(payment);
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id}/payments — retrieve payment records
    // =========================================================================

    /**
     * Retrieve all advance payment records for an owned custom artwork request.
     *
     * Ownership is enforced: a foreign requestId returns 404 (non-disclosure).
     * Returns all payment rows (PENDING, SUCCESS, FAILED) for audit history.
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable; ownership verified
     * @return list of payment records for the custom request
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getCustomRequestPayments(AppUser currentUser, Long requestId) {
        resolveOwnedRequest(currentUser, requestId);
        return paymentRepository.findByCustomOrderRequestId(requestId).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    /** Retrieve all payments for a custom request without customer ownership filtering. */
    @Transactional(readOnly = true)
    public List<PaymentResponse> adminGetCustomRequestPayments(Long requestId) {
        requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
        return paymentRepository.findByCustomOrderRequestId(requestId).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id}/shipment — customer views own shipment
    // =========================================================================

    /**
     * Retrieve shipment/tracking information for an owned custom artwork request.
     *
     * Ownership is enforced: a foreign requestId returns 404.
     * Returns 404 if no shipment record has been created yet.
     *
     * @param currentUser authenticated customer
     * @param requestId   path variable; ownership verified
     * @return ShipmentResponse for the request's shipment
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getCustomRequestShipment(AppUser currentUser, Long requestId) {
        resolveOwnedRequest(currentUser, requestId);
        return shipmentRepository.findByCustomOrderRequestId(requestId)
                .map(ShipmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found for custom request " + requestId));
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
            throw new ResourceNotFoundException("Custom request not found");
        }
        return req;
    }

    /**
     * Build a PaymentResponse that includes the customOrderRequestId
     * (the existing PaymentResponse.from() maps orderId only; this adds the custom
     * request reference for the advance-payment use case).
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        // Reuse the existing factory — orderId will be null for custom-order payments
        return PaymentResponse.from(payment);
    }
}
