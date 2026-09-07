package com.handmadeart.ecommerce.dto.customartwork;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Request DTO for Admin quotation creation.
 *
 * REST API Spec §14 "Admin create quotation":
 *   POST /api/v1/admin/custom-requests/{id}/quotation
 *   Request: QuotationCreateRequest: price, advanceAmount where applicable,
 *            estimatedDelivery, expiry, notes/terms.
 *
 * DEC-005 APPROVED: Admin supplies a positive advance no greater than the quote.
 * No fixed percentage is applied by the server.
 *
 * quoted_amount: CHECK >= 0 in DB; validated here with @DecimalMin.
 * advance_amount: legacy DB permits null/zero; the service enforces DEC-005.
 * expiry_at: must be a future datetime — enforced by the service layer.
 */
public class QuotationCreateRequest {

    /** Total quoted price. CHECK >= 0. NUMERIC(10,2). Required. */
    @NotNull(message = "Quoted amount is required")
    @DecimalMin(value = "0.00", message = "Quoted amount must be >= 0")
    private BigDecimal quotedAmount;

    /**
     * Absolute advance amount. Required and positive.
     * DEC-005 APPROVED: no fixed percentage; Admin enters the exact amount.
     */
    @NotNull(message = "Advance amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Advance amount must be greater than zero")
    private BigDecimal advanceAmount;

    /** Optional estimated delivery date for the artwork. */
    private LocalDate estimatedDeliveryDate;

    /** Quotation expiry. Required — service enforces it is in the future. */
    @NotNull(message = "Expiry date/time is required")
    private OffsetDateTime expiryAt;

    /** Optional notes and terms visible to the customer. */
    private String notesTerms;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public void setQuotedAmount(BigDecimal quotedAmount) { this.quotedAmount = quotedAmount; }

    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(BigDecimal advanceAmount) { this.advanceAmount = advanceAmount; }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public OffsetDateTime getExpiryAt() { return expiryAt; }
    public void setExpiryAt(OffsetDateTime expiryAt) { this.expiryAt = expiryAt; }

    public String getNotesTerms() { return notesTerms; }
    public void setNotesTerms(String notesTerms) { this.notesTerms = notesTerms; }
}
