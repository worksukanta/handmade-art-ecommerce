package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Full response DTO for a custom artwork request.
 *
 * REST API Spec §13:
 *   POST  /api/v1/custom-requests        → 201 Created + CustomArtworkRequestResponse
 *   GET   /api/v1/custom-requests/{id}   → 200 OK + CustomArtworkRequestResponse
 *   PATCH /api/v1/admin/custom-requests/{id}/review → 200 OK + CustomArtworkRequestResponse
 *
 * No internal persistence fields are exposed (SDD §8, REST API Spec §23).
 * Admin-only fields (reviewedBy user ID, reviewNotes) are included — this DTO
 * is returned from both customer and admin read endpoints, but the controller
 * for each role enforces visibility.  Admin review notes are harmless to expose
 * in the customer-facing detail view (the spec does not restrict them).
 */
public class CustomArtworkRequestResponse {

    private Long id;
    private Long userId;
    private String productType;
    private String description;
    private String designTheme;
    private String preferredColors;
    private String dimensionsSize;
    private String budgetRange;
    private LocalDate requiredDeliveryDate;
    private String additionalInstructions;
    private CustomOrderRequestStatus status;
    private Long reviewedByUserId;
    private String reviewNotes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<CustomOrderImageResponse> images;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static CustomArtworkRequestResponse from(CustomOrderRequest req,
                                                     List<CustomOrderImageResponse> images) {
        CustomArtworkRequestResponse dto = new CustomArtworkRequestResponse();
        dto.id = req.getId();
        dto.userId = req.getUser().getId();
        dto.productType = req.getProductType();
        dto.description = req.getDescription();
        dto.designTheme = req.getDesignTheme();
        dto.preferredColors = req.getPreferredColors();
        dto.dimensionsSize = req.getDimensionsSize();
        dto.budgetRange = req.getBudgetRange();
        dto.requiredDeliveryDate = req.getRequiredDeliveryDate();
        dto.additionalInstructions = req.getAdditionalInstructions();
        dto.status = req.getStatus();
        dto.reviewedByUserId = req.getReviewedBy() != null ? req.getReviewedBy().getId() : null;
        dto.reviewNotes = req.getReviewNotes();
        dto.createdAt = req.getCreatedAt();
        dto.updatedAt = req.getUpdatedAt();
        dto.images = images;
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProductType() { return productType; }
    public String getDescription() { return description; }
    public String getDesignTheme() { return designTheme; }
    public String getPreferredColors() { return preferredColors; }
    public String getDimensionsSize() { return dimensionsSize; }
    public String getBudgetRange() { return budgetRange; }
    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public String getAdditionalInstructions() { return additionalInstructions; }
    public CustomOrderRequestStatus getStatus() { return status; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
    public String getReviewNotes() { return reviewNotes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<CustomOrderImageResponse> getImages() { return images; }
}
