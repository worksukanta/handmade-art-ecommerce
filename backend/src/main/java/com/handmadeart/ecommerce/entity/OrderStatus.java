package com.handmadeart.ecommerce.entity;

/**
 * Order lifecycle status values.
 *
 * Approved status model: Database Design &amp; ERD §15.1, SRS §8.1.
 *
 * Mapped as EnumType.STRING so the stored value matches the CHECK constraint
 * in the {@code customer_order.status} column exactly.
 */
public enum OrderStatus {

    /** Order created; payment not yet confirmed successful. */
    PENDING_PAYMENT,

    /** Payment succeeded; order accepted for fulfillment. */
    CONFIRMED,

    /** Order is being prepared for dispatch. */
    PROCESSING,

    /** Order has been dispatched to the customer. */
    SHIPPED,

    /** Order received and confirmed by the customer. */
    DELIVERED,

    /** Order cancelled while in an eligible state (FR-ORD-07). */
    CANCELLED
}
