package com.handmadeart.ecommerce.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Status-aware product summary used only by ADMIN catalogue reads. */
public class AdminProductSummaryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    @JsonProperty("product_type") private String productType;
    private String status;
    @JsonProperty("category_id") private Long categoryId;
    @JsonProperty("category_name") private String categoryName;
    @JsonProperty("primary_image") private ProductImageResponse primaryImage;
    @JsonProperty("created_at") private OffsetDateTime createdAt;

    public static AdminProductSummaryResponse from(Product product, List<ProductImage> images) {
        AdminProductSummaryResponse dto = new AdminProductSummaryResponse();
        dto.id = product.getId(); dto.name = product.getName(); dto.price = product.getPrice();
        dto.productType = product.getProductType().name(); dto.status = product.getStatus().name();
        dto.categoryId = product.getCategory().getId(); dto.categoryName = product.getCategory().getName();
        dto.createdAt = product.getCreatedAt();
        images.stream().filter(ProductImage::isPrimary).findFirst().or(() -> images.stream().findFirst())
                .ifPresent(image -> dto.primaryImage = ProductImageResponse.from(image));
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getProductType() { return productType; }
    public String getStatus() { return status; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public ProductImageResponse getPrimaryImage() { return primaryImage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
