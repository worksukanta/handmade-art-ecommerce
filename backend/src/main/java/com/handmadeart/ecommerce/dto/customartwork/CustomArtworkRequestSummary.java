package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;

import java.time.OffsetDateTime;

/**
 * Summary response DTO for a custom artwork request list item.
 *
 * REST API Spec §13:
 *   GET /api/v1/custom-requests          → 200 OK + PageResponse&lt;CustomArtworkRequestSummary&gt;
 *   GET /api/v1/admin/custom-requests    → 200 OK + PageResponse&lt;CustomArtworkRequestSummary&gt;
 *
 * Contains only the fields needed for list views (id, type, status, timestamps).
 * Does not expose reference images or review notes at summary level.
 */
public class CustomArtworkRequestSummary {

    private Long id;
    private Long userId;
    private String productType;
    private String description;
    private CustomOrderRequestStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static CustomArtworkRequestSummary from(CustomOrderRequest req) {
        CustomArtworkRequestSummary dto = new CustomArtworkRequestSummary();
        dto.id = req.getId();
        dto.userId = req.getUser().getId();
        dto.productType = req.getProductType();
        dto.description = req.getDescription();
        dto.status = req.getStatus();
        dto.createdAt = req.getCreatedAt();
        dto.updatedAt = req.getUpdatedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProductType() { return productType; }
    public String getDescription() { return description; }
    public CustomOrderRequestStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
