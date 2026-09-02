package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Admin updating a shipment's status.
 *
 * REST API Spec §15 "Admin update shipment status":
 *   PATCH /api/v1/admin/shipments/{id}/status
 *
 * DEC-008 APPROVED: approved transitions PENDING → SHIPPED → DELIVERED.
 */
public class ShipmentStatusUpdateRequest {

    /**
     * New shipment status. Must be a valid next state per approved transitions
     * (ERD §15.7): PENDING → SHIPPED → DELIVERED.
     */
    @NotNull(message = "status is required")
    private ShipmentStatus status;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
}
