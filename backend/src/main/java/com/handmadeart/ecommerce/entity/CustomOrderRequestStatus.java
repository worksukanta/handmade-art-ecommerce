package com.handmadeart.ecommerce.entity;

/**
 * Lifecycle status values for a {@link CustomOrderRequest}.
 *
 * Thirteen values defined by SRS §8.2 and Database Design &amp; ERD §15.3.
 * Persisted as the enum name string via {@code @Enumerated(EnumType.STRING)},
 * matching the CHECK constraint in the {@code custom_order_request} table.
 *
 * Approved transitions (ERD §15.3):
 * <pre>
 *   REQUESTED → UNDER_REVIEW | CANCELLED
 *   UNDER_REVIEW → QUOTED | REJECTED | CANCELLED
 *   QUOTED → CUSTOMER_APPROVAL_PENDING
 *   CUSTOMER_APPROVAL_PENDING → APPROVED | REJECTED | QUOTATION_EXPIRED
 *   APPROVED → ADVANCE_PAYMENT_PENDING
 *   ADVANCE_PAYMENT_PENDING → IN_PRODUCTION
 *   IN_PRODUCTION → COMPLETED
 *   COMPLETED → SHIPPED
 *   SHIPPED → DELIVERED
 *   REJECTED, QUOTATION_EXPIRED, CANCELLED, DELIVERED → (terminal)
 * </pre>
 *
 * State-transition validation is enforced by the service layer, not the database.
 * The database CHECK constraint only guarantees the value is one of the thirteen
 * approved strings (ERD §12.3).
 */
public enum CustomOrderRequestStatus {

    /** Submitted by the customer; awaiting Admin review. */
    REQUESTED,

    /** Admin is evaluating feasibility (FR-CUST-05). */
    UNDER_REVIEW,

    /** Admin has issued a Quotation (FR-CUST-07). */
    QUOTED,

    /** Quotation presented to customer; awaiting approve/reject decision. */
    CUSTOMER_APPROVAL_PENDING,

    /** Customer approved the quotation (FR-CUST-09). */
    APPROVED,

    /** Awaiting successful advance payment. */
    ADVANCE_PAYMENT_PENDING,

    /** Artist is producing the artwork. */
    IN_PRODUCTION,

    /** Production finished. */
    COMPLETED,

    /** Dispatched to the customer. */
    SHIPPED,

    /** Received by the customer — terminal success state. */
    DELIVERED,

    /** Rejected by Admin during review, or by the customer at approval — terminal. */
    REJECTED,

    /** Quotation's expiry_at passed before a decision (BR-06) — terminal. */
    QUOTATION_EXPIRED,

    /** Cancelled by the customer or Admin while in an eligible early state — terminal. */
    CANCELLED
}
