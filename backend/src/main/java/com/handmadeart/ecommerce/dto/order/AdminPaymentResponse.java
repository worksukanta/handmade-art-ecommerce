package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Admin payment detail response.
 *
 * Extends the standard PaymentResponse with the customOrderRequestId field
 * so admins can view payments for both ready-made orders and custom requests.
 *
 * REST API Spec §12: GET /api/v1/admin/payments/{id}.
 *
 * Security invariants (FR-PAY-04, NFR-07):
 *   - Never exposes raw card number, CVV, PIN, or provider authentication secrets.
 *   - Only the provider's transaction reference string and outcome are exposed.
 */
public class AdminPaymentResponse {

    private Long paymentId;
    private Long orderId;
    private Long customOrderRequestId;
    private PaymentPurpose paymentPurpose;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String providerTransactionReference;
    private String failureReason;
    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;

    public AdminPaymentResponse() {
    }

    public static AdminPaymentResponse from(Payment payment) {
        AdminPaymentResponse dto = new AdminPaymentResponse();
        dto.paymentId = payment.getId();
        dto.orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;
        dto.customOrderRequestId = payment.getCustomOrderRequest() != null
                ? payment.getCustomOrderRequest().getId() : null;
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

    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public Long getCustomOrderRequestId() { return customOrderRequestId; }
    public PaymentPurpose getPaymentPurpose() { return paymentPurpose; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderTransactionReference() { return providerTransactionReference; }
    public String getFailureReason() { return failureReason; }
    public OffsetDateTime getInitiatedAt() { return initiatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
