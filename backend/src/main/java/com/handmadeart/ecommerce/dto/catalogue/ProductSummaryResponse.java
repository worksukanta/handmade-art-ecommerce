package com.handmadeart.ecommerce.dto.catalogue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Compact product representation for the catalogue listing endpoint.
 *
 * Returned by:
 *   GET /api/v1/products              — page content items
 *   GET /api/v1/products/{id}/related-products — list items
 *
 * Approved fields (REST API Spec §17, ProductSummaryResponse):
 *   id, name, price, product_type, category_id, category_name, primary_image, created_at.
 *
 * Availability is omitted from the summary (present in ProductDetailResponse).
 * Internal fields (status enum raw, DB timestamps other than created_at) are not included.
 */
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private BigDecimal price;

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("primary_image")
    private ProductImageResponse primaryImage;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public ProductSummaryResponse() {
    }

    /**
     * Map a Product entity to a summary DTO.
     *
     * @param product the product (category must be loaded — not a proxy)
     * @param images  the product's images; the first primary one is used as thumbnail
     */
    public static ProductSummaryResponse from(Product product, List<ProductImage> images) {
        ProductSummaryResponse dto = new ProductSummaryResponse();
        dto.id = product.getId();
        dto.name = product.getName();
        dto.price = product.getPrice();
        dto.productType = product.getProductType().name();
        dto.categoryId = product.getCategory().getId();
        dto.categoryName = product.getCategory().getName();
        dto.createdAt = product.getCreatedAt();

        // Use first primary image, or first image if none is flagged primary
        images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .ifPresent(img -> dto.primaryImage = ProductImageResponse.from(img));

        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getProductType() { return productType; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public ProductImageResponse getPrimaryImage() { return primaryImage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
