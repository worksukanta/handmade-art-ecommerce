package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response DTO for a quotation.
 *
 * REST API Spec §14:
 *   POST /api/v1/admin/custom-requests/{id}/quotation → 201 Created + QuotationResponse
 *   GET  /api/v1/custom-requests/{id}/quotation       → 200 OK + QuotationResponse
 *   GET  /api/v1/admin/quotations/{id}                → 200 OK + QuotationResponse
 *
 * DEC-005 OPEN: advanceAmount is an Admin-entered absolute value; no fixed percentage.
 * Internal fields not exposed to customers: none — the spec does not restrict fields here.
 * Raw payment-card information and provider secrets are never part of this DTO.
 */
public class QuotationResponse {

    private Long id;
    private Long customOrderRequestId;
    private BigDecimal quotedAmount;
    private BigDecimal advanceAmount;
    private LocalDate estimatedDeliveryDate;
    private OffsetDateTime expiryAt;
    private String notesTerms;
    private QuotationStatus status;
    private Long createdByUserId;
    private OffsetDateTime createdAt;
    private OffsetDateTime decidedAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static QuotationResponse from(Quotation q) {
        QuotationResponse dto = new QuotationResponse();
        dto.id = q.getId();
        dto.customOrderRequestId = q.getCustomOrderRequest().getId();
        dto.quotedAmount = q.getQuotedAmount();
        dto.advanceAmount = q.getAdvanceAmount();
        dto.estimatedDeliveryDate = q.getEstimatedDeliveryDate();
        dto.expiryAt = q.getExpiryAt();
        dto.notesTerms = q.getNotesTerms();
        dto.status = q.getStatus();
        dto.createdByUserId = q.getCreatedBy().getId();
        dto.createdAt = q.getCreatedAt();
        dto.decidedAt = q.getDecidedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getCustomOrderRequestId() { return customOrderRequestId; }
    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public OffsetDateTime getExpiryAt() { return expiryAt; }
    public String getNotesTerms() { return notesTerms; }
    public QuotationStatus getStatus() { return status; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
}
