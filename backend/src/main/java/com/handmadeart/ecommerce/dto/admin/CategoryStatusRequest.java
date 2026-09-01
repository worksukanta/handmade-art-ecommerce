package com.handmadeart.ecommerce.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a category's status (activate / deactivate).
 *
 * Used by:
 *   PATCH /api/v1/admin/categories/{id}/status
 *
 * Approved field (REST API Spec §6, tbl[18] CategoryStatusRequest):
 *   status — required; must be a valid CategoryStatus value (ACTIVE / INACTIVE)
 */
public class CategoryStatusRequest {

    @NotNull(message = "Status is required")
    private String status;

    public CategoryStatusRequest() {
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
