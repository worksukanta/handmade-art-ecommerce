package com.handmadeart.ecommerce.entity;

/**
 * Payment transaction status values.
 *
 * Approved status model: Database Design &amp; ERD §15.2, FR-PAY-03.
 *
 * A failed payment row is terminal — a retry creates a new Payment row
 * referencing the same Order or CustomOrderRequest (ERD §15.2).
 */
public enum PaymentStatus {

    /** Payment initiated; outcome not yet returned by the provider. */
    PENDING,

    /** Payment provider confirmed success. Terminal for this payment row. */
    SUCCESS,

    /** Payment provider declined or the attempt errored. Terminal for this row. */
    FAILED
}
