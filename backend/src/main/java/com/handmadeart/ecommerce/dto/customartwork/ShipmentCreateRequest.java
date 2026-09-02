package com.handmadeart.ecommerce.dto.customartwork;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for Admin creating or updating a shipment record.
 *
 * REST API Spec §15 "Admin create/update shipment":
 *   POST /api/v1/admin/shipments — create a shipment for a custom request or order
 *
 * DEC-008 APPROVED: MVP shipping is status/tracking based; no carrier API integration.
 *
 * Exactly one of {@code customOrderRequestId} or {@code orderId} must be provided.
 */
public class ShipmentCreateRequest {

    /**
     * The custom order request to create a shipment for.
     * Mutually exclusive with orderId.
     */
    private Long customOrderRequestId;

    /**
     * The ready-made order to create a shipment for.
     * Mutually exclusive with customOrderRequestId.
     */
    private Long orderId;

    /** Optional carrier name (free-text, DEC-008). */
    private String carrierName;

    /** Optional tracking reference returned by carrier (free-text, FR-SHIP-02). */
    private String trackingReference;

    /** Optional estimated delivery date (FR-SHIP-03). */
    private LocalDate estimatedDeliveryDate;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getCustomOrderRequestId() { return customOrderRequestId; }
    public void setCustomOrderRequestId(Long customOrderRequestId) {
        this.customOrderRequestId = customOrderRequestId;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

    public String getTrackingReference() { return trackingReference; }
    public void setTrackingReference(String trackingReference) {
        this.trackingReference = trackingReference;
    }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
}
