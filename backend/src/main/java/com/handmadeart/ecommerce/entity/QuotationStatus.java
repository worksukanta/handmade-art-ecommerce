package com.handmadeart.ecommerce.entity;

/**
 * Status values for a {@link Quotation}.
 *
 * Four values defined by Database Design &amp; ERD §15.4.
 * Persisted as the enum name string via {@code @Enumerated(EnumType.STRING)},
 * matching the CHECK constraint in the {@code quotation} table.
 *
 * Approved transitions (ERD §15.4):
 * <pre>
 *   PENDING → APPROVED | REJECTED | EXPIRED
 *   APPROVED, REJECTED, EXPIRED → (terminal)
 * </pre>
 *
 * Once a Quotation's {@code expiry_at} timestamp has passed, the service layer
 * transitions status to EXPIRED and blocks any further approval attempts
 * (BR-06, FR-CUST-10, ERD §13.3).
 */
public enum QuotationStatus {

    /** Issued; awaiting the customer's decision. */
    PENDING,

    /** Customer approved before expiry — terminal. */
    APPROVED,

    /** Customer explicitly rejected — terminal. */
    REJECTED,

    /** expiry_at passed with no decision; approval is blocked — terminal. */
    EXPIRED
}
