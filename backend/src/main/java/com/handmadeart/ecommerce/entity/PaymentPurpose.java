package com.handmadeart.ecommerce.entity;

/**
 * Payment purpose discriminator.
 *
 * Distinguishes a ready-made full payment from custom-artwork advance
 * and remaining payments (FR-PAY-05, Database Design &amp; ERD §3.11).
 */
public enum PaymentPurpose {

    /** Full payment for a ready-made order. */
    FULL,

    /** Advance payment for an approved custom artwork quotation. */
    ADVANCE,

    /** Remaining balance payment for a completed custom artwork order. */
    REMAINING
}
