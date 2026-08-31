package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code quotation} table.
 *
 * Represents a commercial proposal issued by an Admin for a custom artwork request.
 * Enforces expiry and approval/rejection lifecycle (FR-CUST-07..10, BR-06,
 * Database Design &amp; ERD §3.15, §13).
 *
 * Key design decisions (ERD §13):
 * <ul>
 *   <li>One quotation per request — {@code customOrderRequest} FK is UNIQUE (ERD §13.2,
 *       DEC-004 DEFERRED: re-quotation not implemented).</li>
 *   <li>{@code advanceAmount} is nullable, stored as an Admin-entered absolute value —
 *       no fixed percentage is hard-coded (ERD §13.4, DEC-005 OPEN).</li>
 *   <li>{@code expiryAt} is NOT NULL; the service layer compares it against current time
 *       before processing an approval (ERD §13.3, BR-06, FR-CUST-10).</li>
 *   <li>{@code decidedAt} is application-managed (nullable); set when the customer
 *       approves or rejects.</li>
 * </ul>
 *
 * Security rule: monetary fields use {@link BigDecimal} (NUMERIC(10,2)) for exact
 * decimal arithmetic — never float/double.
 *
 * Approved schema source: Database Design &amp; ERD §3.15, §13, §15.4.
 */
@Entity
@Table(name = "quotation")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * The custom request this quotation belongs to.
     * UNIQUE FK — one quotation per request (ERD §13.2).
     * ON DELETE RESTRICT: a request with a quotation cannot be hard-deleted outright.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "custom_order_request_id", nullable = false, unique = true)
    private CustomOrderRequest customOrderRequest;

    /**
     * Total quoted price for the artwork. CHECK &gt;= 0. NUMERIC(10,2) (FR-CUST-08).
     */
    @Column(name = "quoted_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedAmount;

    /**
     * Absolute advance amount entered by Admin. Nullable.
     * CHECK advance_amount IS NULL OR advance_amount &gt;= 0.
     * No fixed percentage — DEC-005 OPEN (ERD §13.4).
     */
    @Column(name = "advance_amount", precision = 10, scale = 2)
    private BigDecimal advanceAmount;

    /**
     * Estimated delivery date for the artwork. Nullable (FR-CUST-08).
     */
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    /**
     * Expiry timestamp — NOT NULL. Expired quotations block approval (BR-06, FR-CUST-10).
     * Compared against current time by the service layer at approval time (ERD §13.3).
     */
    @Column(name = "expiry_at", nullable = false)
    private OffsetDateTime expiryAt;

    /**
     * Admin-entered notes and terms. Nullable (FR-CUST-08).
     */
    @Column(name = "notes_terms", columnDefinition = "TEXT")
    private String notesTerms;

    /**
     * Quotation status lifecycle (ERD §15.4). DEFAULT 'PENDING'.
     * Four approved values: PENDING, APPROVED, REJECTED, EXPIRED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private QuotationStatus status;

    /**
     * Admin who issued the quotation — FK to {@code app_user.id}, NOT NULL (FR-CUST-07).
     * ON DELETE RESTRICT: an Admin who issued quotations cannot be deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    // DB DEFAULT now() is authoritative. insertable = false omits from INSERT.
    // @Generated(INSERT): Hibernate re-SELECTs after INSERT to populate the field.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    /**
     * When the customer approved or rejected. Application-managed, nullable.
     * Set explicitly by the service layer on a customer decision.
     */
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Quotation() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public CustomOrderRequest getCustomOrderRequest() { return customOrderRequest; }
    public void setCustomOrderRequest(CustomOrderRequest customOrderRequest) {
        this.customOrderRequest = customOrderRequest;
    }

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

    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }

    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
}
