package com.handmadeart.ecommerce.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a product's status (activate / deactivate).
 *
 * Used by:
 *   PATCH /api/v1/admin/products/{id}/status
 *
 * Approved field (REST API Spec §7, tbl[24] ProductStatusRequest):
 *   status — required; must be a valid ProductStatus value (ACTIVE / INACTIVE)
 */
public class ProductStatusRequest {

    @NotNull(message = "Status is required")
    private String status;

    public ProductStatusRequest() {
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
