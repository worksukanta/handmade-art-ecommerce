package com.handmadeart.ecommerce.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.Inventory;

import java.time.OffsetDateTime;

/**
 * Response DTO for inventory management operations.
 *
 * Returned by:
 *   GET   /api/v1/admin/inventory
 *   GET   /api/v1/admin/inventory/{productId}
 *   PATCH /api/v1/admin/inventory/{productId}
 *
 * Approved shape (REST API Spec §12, tbl[45-47] InventoryResponse):
 *   product_id, quantity_on_hand, updated_at
 *
 * DEC-009 (inventory concurrency strategy) remains OPEN.
 */
public class InventoryResponse {

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("quantity_on_hand")
    private Integer quantityOnHand;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public InventoryResponse() {
    }

    public static InventoryResponse from(Inventory inventory) {
        InventoryResponse dto = new InventoryResponse();
        dto.productId = inventory.getProductId();
        dto.quantityOnHand = inventory.getQuantityOnHand();
        dto.updatedAt = inventory.getUpdatedAt();
        return dto;
    }

    public Long getProductId() { return productId; }
    public Integer getQuantityOnHand() { return quantityOnHand; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
