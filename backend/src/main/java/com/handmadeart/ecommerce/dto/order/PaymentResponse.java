package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for a payment record.
 *
 * REST API Spec §11 "Initiate order payment" / "Get order payments":
 *   DTO table: "id, purpose, amount, method label, status, provider reference,
 *               timestamps/failure reason where safe"
 *
 * Security:
 *   Never exposes raw card number, CVV, PIN, or provider authentication secrets.
 *   Only the provider's transaction reference string and outcome status are included
 *   (FR-PAY-02, FR-PAY-04, NFR-07, DEC-001 DEFERRED).
 *
 * DEC-001 DEFERRED: provider-agnostic / mock/sandbox behavior.
 * The {@code providerTransactionReference} is included where the provider has returned one.
 * {@code failureReason} is included only when status = FAILED.
 */
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private PaymentPurpose paymentPurpose;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String providerTransactionReference;  // null until provider responds
    private String failureReason;                 // null unless FAILED
    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;           // null while PENDING

    public PaymentResponse() {
    }

    /**
     * Build a PaymentResponse from a persisted {@link Payment} entity.
     *
     * @param payment the payment entity; its order FK must be non-null (standard orders only)
     */
    public static PaymentResponse from(Payment payment) {
        PaymentResponse dto = new PaymentResponse();
        dto.paymentId = payment.getId();
        dto.orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;
        dto.paymentPurpose = payment.getPaymentPurpose();
        dto.amount = payment.getAmount();
        dto.paymentMethod = payment.getPaymentMethod();
        dto.status = payment.getStatus();
        dto.providerTransactionReference = payment.getProviderTransactionReference();
        dto.failureReason = payment.getFailureReason();
        dto.initiatedAt = payment.getInitiatedAt();
        dto.completedAt = payment.getCompletedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public PaymentPurpose getPaymentPurpose() { return paymentPurpose; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderTransactionReference() { return providerTransactionReference; }
    public String getFailureReason() { return failureReason; }
    public OffsetDateTime getInitiatedAt() { return initiatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
