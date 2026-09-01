package com.handmadeart.ecommerce.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating inventory stock.
 *
 * Used by:
 *   PATCH /api/v1/admin/inventory/{productId}
 *
 * Approved fields (REST API Spec §12, tbl[47] InventoryUpdateRequest):
 *   availableQuantity — required; must be >= 0 (REST API Spec §18, BR-15)
 *
 * DEC-009 (inventory concurrency strategy) remains OPEN.
 * This DTO performs basic admin stock-level management only — no reservation,
 * no locking, no concurrency mechanism introduced here.
 */
public class InventoryUpdateRequest {

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    public InventoryUpdateRequest() {
    }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
