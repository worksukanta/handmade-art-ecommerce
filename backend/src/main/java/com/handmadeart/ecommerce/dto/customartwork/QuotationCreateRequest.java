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
 * DEC-005 OPEN: advanceAmount is an optional absolute value supplied by the Admin.
 * No fixed percentage is applied by the server. The field is accepted if provided
 * but is never required.
 *
 * quoted_amount: CHECK >= 0 in DB; validated here with @DecimalMin.
 * advance_amount: CHECK IS NULL OR >= 0 in DB; validated if present.
 * expiry_at: must be a future datetime — enforced by the service layer.
 */
public class QuotationCreateRequest {

    /** Total quoted price. CHECK >= 0. NUMERIC(10,2). Required. */
    @NotNull(message = "Quoted amount is required")
    @DecimalMin(value = "0.00", message = "Quoted amount must be >= 0")
    private BigDecimal quotedAmount;

    /**
     * Absolute advance amount. Optional. CHECK >= 0 if present.
     * DEC-005 OPEN: no fixed percentage; Admin enters the exact amount.
     */
    @DecimalMin(value = "0.00", message = "Advance amount must be >= 0")
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
