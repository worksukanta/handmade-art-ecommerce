package com.handmadeart.ecommerce.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for initiating a payment on a standard order.
 *
 * REST API Spec §11 "Initiate order payment":
 *   "PaymentInitiationRequest with approved non-sensitive method/provider metadata only"
 *   "Never card number/CVV/PIN."
 *
 * DEC-001 DEFERRED: provider-agnostic / mock/sandbox.
 * Only a generic payment method label is accepted (e.g., "CARD", "UPI", "SANDBOX").
 * No provider-specific authentication fields are stored.
 *
 * Backend derives the authoritative payment amount from the stored order total.
 * Client-supplied amounts are not accepted.
 */
public class PaymentInitiationRequest {

    /**
     * Generic payment method label (e.g., "CARD", "UPI", "SANDBOX").
     * Not a provider authentication credential.
     * Max 30 characters to match the {@code payment.payment_method} column constraint.
     */
    @NotBlank(message = "paymentMethod is required")
    @Size(max = 30, message = "paymentMethod must not exceed 30 characters")
    private String paymentMethod;

    public PaymentInitiationRequest() {
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
