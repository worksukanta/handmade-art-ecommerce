package com.handmadeart.ecommerce.dto.customartwork;

import com.handmadeart.ecommerce.entity.CustomOrderImage;

import java.time.OffsetDateTime;

/**
 * Response DTO for a reference image attached to a custom order request.
 *
 * REST API Spec §13 "Upload reference image":
 *   POST /api/v1/custom-requests/{id}/images → 201 Created + CustomOrderImageResponse
 *
 * Never exposes arbitrary filesystem paths (SDD §13.3, DEC-003).
 * {@code storageReference} is a logical path only — never a raw OS path.
 */
public class CustomOrderImageResponse {

    private Long id;
    private Long customOrderRequestId;
    private String storageReference;
    private String originalFilename;
    private String contentType;
    private Integer fileSizeBytes;
    private OffsetDateTime uploadedAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static CustomOrderImageResponse from(CustomOrderImage image) {
        CustomOrderImageResponse dto = new CustomOrderImageResponse();
        dto.id = image.getId();
        dto.customOrderRequestId = image.getCustomOrderRequest().getId();
        dto.storageReference = image.getStorageReference();
        dto.originalFilename = image.getOriginalFilename();
        dto.contentType = image.getContentType();
        dto.fileSizeBytes = image.getFileSizeBytes();
        dto.uploadedAt = image.getUploadedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public Long getCustomOrderRequestId() { return customOrderRequestId; }
    public String getStorageReference() { return storageReference; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public Integer getFileSizeBytes() { return fileSizeBytes; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
}
