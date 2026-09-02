package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.entity.ShipmentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response DTO for a shipment record.
 *
 * REST API Spec §15:
 *   GET /api/v1/custom-requests/{id}/shipment — customer view own custom request shipment
 *   GET /api/v1/orders/{id}/shipment           — customer view own order shipment
 *   POST/PUT /api/v1/admin/shipments           — admin create/update shipment
 *   PATCH /api/v1/admin/shipments/{id}/status  — admin update shipment status
 *
 * DEC-008 APPROVED: MVP shipping is status/tracking based; no carrier API integration.
 * Exposes: id, parent references, carrier, tracking reference, status, timestamps.
 * Does not expose filesystem paths or internal infrastructure details.
 */
public class ShipmentResponse {

    private Long id;
    private Long orderId;
    private Long customOrderRequestId;
    private String carrierName;
    private String trackingReference;
    private ShipmentStatus status;
    private LocalDate estimatedDeliveryDate;
    private OffsetDateTime shippedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static ShipmentResponse from(Shipment s) {
        ShipmentResponse dto = new ShipmentResponse();
        dto.id = s.getId();
        dto.orderId = s.getOrder() != null ? s.getOrder().getId() : null;
        dto.customOrderRequestId = s.getCustomOrderRequest() != null
                ? s.getCustomOrderRequest().getId() : null;
        dto.carrierName = s.getCarrierName();
        dto.trackingReference = s.getTrackingReference();
        dto.status = s.getStatus();
        dto.estimatedDeliveryDate = s.getEstimatedDeliveryDate();
        dto.shippedAt = s.getShippedAt();
        dto.deliveredAt = s.getDeliveredAt();
        dto.createdAt = s.getCreatedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getCustomOrderRequestId() { return customOrderRequestId; }
    public String getCarrierName() { return carrierName; }
    public String getTrackingReference() { return trackingReference; }
    public ShipmentStatus getStatus() { return status; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public OffsetDateTime getShippedAt() { return shippedAt; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
