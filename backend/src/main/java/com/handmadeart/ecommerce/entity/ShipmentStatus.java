package com.handmadeart.ecommerce.entity;

/**
 * Status values for a {@link Shipment}.
 *
 * Three values defined by Database Design &amp; ERD §15.7 and FR-SHIP-02.
 * Persisted as the enum name string via {@code @Enumerated(EnumType.STRING)},
 * matching the CHECK constraint in the {@code shipment} table.
 *
 * Approved transitions (ERD §15.7):
 * <pre>
 *   PENDING → SHIPPED → DELIVERED (terminal)
 * </pre>
 */
public enum ShipmentStatus {

    /** Shipment record created; not yet dispatched. */
    PENDING,

    /** Dispatched; {@code tracking_reference} populated where available (FR-SHIP-02). */
    SHIPPED,

    /** Received by the customer — terminal state. */
    DELIVERED
}
