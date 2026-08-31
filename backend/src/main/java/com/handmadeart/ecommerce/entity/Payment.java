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
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code payment} table.
 *
 * Records a payment transaction for either a ready-made order or a custom-artwork
 * commission (advance or remaining payment). A single table serves both journeys
 * via two nullable FKs, exactly one of which must be set per row — enforced by a
 * database CHECK constraint (Database Design &amp; ERD §10.2).
 *
 * Security rule: this entity NEVER stores raw card number, CVV, PIN, or any
 * card-authentication secret. Only the provider's transaction reference string
 * and a success/failure outcome are retained (FR-PAY-04, NFR-07, BR-13, ERD §10.3,
 * DEC-001 DEFERRED).
 *
 * Design notes:
 * <ul>
 *   <li>{@code order} is set for ready-made full payments (payment_purpose = FULL).</li>
 *   <li>{@code customOrderRequest} is set for custom advance/remaining payments.</li>
 *   <li>One-to-many: an order may have multiple Payment rows (e.g., a FAILED attempt
 *       followed by a SUCCESS) — a failed row is terminal; a retry is a new row.</li>
 *   <li>{@code completedAt} is application-managed: set when a SUCCESS or FAILED
 *       outcome is recorded. Not DB-DEFAULT-generated.</li>
 *   <li>{@code initiatedAt}: DB DEFAULT now() — @Generated(INSERT) applied.</li>
 *   <li>Phase 2E: {@code customOrderRequest} upgraded from a raw Long column to a
 *       proper {@code @ManyToOne} reference now that {@link CustomOrderRequest} exists.
 *       The deferred FK constraint is added to V5 migration.</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.11, §10.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning order — nullable FK to {@code customer_order.id}.
     * Set for FULL ready-made payments; null for custom-order payments.
     * Exactly one of {@code order} / {@code customOrderRequest} must be non-null
     * — enforced by a database CHECK constraint (ERD §3.11).
     * ON DELETE RESTRICT: an order with payment rows cannot be hard-deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private CustomerOrder order;

    /**
     * Owning custom order request — nullable FK to {@code custom_order_request.id}.
     * Set for ADVANCE / REMAINING custom-order payments; null for ready-made orders.
     * Exactly one of {@code order} / {@code customOrderRequest} must be non-null
     * — enforced by a database CHECK constraint (ERD §3.11).
     * The FK constraint is added in V5 migration (deferred from V4, ERD §10.2).
     * ON DELETE RESTRICT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_order_request_id", nullable = true)
    private CustomOrderRequest customOrderRequest;

    /**
     * Distinguishes ready-made full payment from custom advance/remaining (FR-PAY-05).
     * Persisted as the enum name string matching the CHECK constraint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_purpose", nullable = false, length = 15)
    private PaymentPurpose paymentPurpose;

    /**
     * Payment amount. CHECK amount >= 0. NUMERIC(10,2).
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Generic payment method label (e.g., "CARD", "UPI", "SANDBOX").
     * Exact provider is an open decision (DEC-001 DEFERRED, ERD §10.4).
     */
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    /**
     * Transaction outcome: PENDING → SUCCESS or FAILED (FR-PAY-03).
     * DEFAULT 'PENDING'. A failed row is terminal; a retry is a new Payment row.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status;

    /**
     * Reference returned by the payment provider (FR-PAY-02).
     * Unique (partial — only when not null) to prevent two rows claiming the same
     * provider reference. Null until the provider responds.
     */
    @Column(name = "provider_transaction_reference", nullable = true, length = 150)
    private String providerTransactionReference;

    /**
     * Failure description. Populated only when {@code status = FAILED}.
     */
    @Column(name = "failure_reason", nullable = true, length = 255)
    private String failureReason;

    // DB DEFAULT now() is authoritative. insertable = false omits from INSERT.
    // @Generated(INSERT): Hibernate re-SELECTs after INSERT to populate the field.
    @Generated(event = EventType.INSERT)
    @Column(name = "initiated_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime initiatedAt;

    /**
     * When a success/failure outcome was recorded (application-managed, nullable).
     * Not DB-DEFAULT-generated; set explicitly by the service layer on outcome.
     */
    @Column(name = "completed_at", nullable = true)
    private OffsetDateTime completedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Payment() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public CustomOrderRequest getCustomOrderRequest() { return customOrderRequest; }
    public void setCustomOrderRequest(CustomOrderRequest customOrderRequest) {
        this.customOrderRequest = customOrderRequest;
    }

    public PaymentPurpose getPaymentPurpose() { return paymentPurpose; }
    public void setPaymentPurpose(PaymentPurpose paymentPurpose) { this.paymentPurpose = paymentPurpose; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getProviderTransactionReference() { return providerTransactionReference; }
    public void setProviderTransactionReference(String ref) { this.providerTransactionReference = ref; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public OffsetDateTime getInitiatedAt() { return initiatedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
