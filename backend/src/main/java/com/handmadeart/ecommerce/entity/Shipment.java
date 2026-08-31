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

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code shipment} table.
 *
 * Records fulfilment/shipping information for either a completed ready-made order
 * or a completed custom-artwork commission.
 *
 * A single table serves both journeys via two nullable FKs ({@code order} /
 * {@code customOrderRequest}), exactly one of which must be set per row — enforced
 * by a database CHECK constraint (Database Design &amp; ERD §3.16, §10.2 pattern).
 *
 * Key design decisions (ERD §3.16, DEC-008 APPROVED):
 * <ul>
 *   <li>MVP shipping is status/tracking-based only — no external carrier API required.</li>
 *   <li>{@code carrierName} and {@code trackingReference} are free-text; no automated
 *       logistics integration (FR-SHIP-04).</li>
 *   <li>{@code shippedAt} and {@code deliveredAt} are application-managed nullables;
 *       the service layer sets them when status advances to SHIPPED or DELIVERED.</li>
 *   <li>{@code createdAt} uses DB DEFAULT now() via {@code @Generated(INSERT)}.</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.16, §15.7.
 */
@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning ready-made order — nullable FK to {@code customer_order.id}.
     * Set for ready-made order fulfilment; null for custom-artwork shipments.
     * Exactly one of {@code order} / {@code customOrderRequest} must be non-null
     * — enforced by a database CHECK constraint (ERD §3.16).
     * ON DELETE RESTRICT: an order with a shipment record cannot be hard-deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private CustomerOrder order;

    /**
     * Owning custom order request — nullable FK to {@code custom_order_request.id}.
     * Set for completed custom-artwork fulfilment; null for ready-made order shipments.
     * ON DELETE RESTRICT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_order_request_id", nullable = true)
    private CustomOrderRequest customOrderRequest;

    /**
     * Carrier name (free-text in MVP, FR-SHIP-04 / DEC-008 APPROVED).
     * No automated carrier integration for MVP.
     */
    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    /**
     * Carrier tracking reference (FR-SHIP-02). Nullable until dispatched.
     */
    @Column(name = "tracking_reference", length = 150)
    private String trackingReference;

    /**
     * Shipment status (ERD §15.7). DEFAULT 'PENDING'.
     * Three approved values: PENDING, SHIPPED, DELIVERED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ShipmentStatus status;

    /**
     * Estimated delivery date (FR-SHIP-03). Nullable.
     */
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    /**
     * When status became SHIPPED. Application-managed, nullable.
     * Set by the service layer when shipment is dispatched.
     */
    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    /**
     * When status became DELIVERED. Application-managed, nullable.
     * Set by the service layer on delivery confirmation.
     */
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    // DB DEFAULT now() is authoritative. insertable = false omits from INSERT.
    // @Generated(INSERT): Hibernate re-SELECTs after INSERT to populate the field.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Shipment() {
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

    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

    public String getTrackingReference() { return trackingReference; }
    public void setTrackingReference(String trackingReference) { this.trackingReference = trackingReference; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public OffsetDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(OffsetDateTime shippedAt) { this.shippedAt = shippedAt; }

    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
