package com.handmadeart.ecommerce.dto.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.ProductImage;

/**
 * Response DTO for a single product image.
 *
 * Included inside ProductDetailResponse.
 *
 * Approved fields (REST API Spec §17, ProductDetailResponse):
 *   id, storage_reference, original_filename, content_type,
 *   file_size_bytes, display_order, is_primary.
 *
 * Internal fields (product_id FK) are not exposed.
 */
public class ProductImageResponse {

    private Long id;

    @JsonProperty("storage_reference")
    private String storageReference;

    @JsonProperty("original_filename")
    private String originalFilename;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("file_size_bytes")
    private Integer fileSizeBytes;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("is_primary")
    private boolean isPrimary;

    public ProductImageResponse() {
    }

    public static ProductImageResponse from(ProductImage image) {
        ProductImageResponse dto = new ProductImageResponse();
        dto.id = image.getId();
        dto.storageReference = image.getStorageReference();
        dto.originalFilename = image.getOriginalFilename();
        dto.contentType = image.getContentType();
        dto.fileSizeBytes = image.getFileSizeBytes();
        dto.displayOrder = image.getDisplayOrder();
        dto.isPrimary = image.isPrimary();
        return dto;
    }

    public Long getId() { return id; }
    public String getStorageReference() { return storageReference; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public Integer getFileSizeBytes() { return fileSizeBytes; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPrimary() { return isPrimary; }
}
