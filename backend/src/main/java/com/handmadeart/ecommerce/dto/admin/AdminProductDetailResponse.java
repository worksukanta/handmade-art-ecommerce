package com.handmadeart.ecommerce.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Full authoritative product representation for ADMIN catalogue management. */
public class AdminProductDetailResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    @JsonProperty("product_type") private String productType;
    private String status;
    @JsonProperty("category_id") private Long categoryId;
    @JsonProperty("category_name") private String categoryName;
    private List<ProductImageResponse> images;
    private ProductDetailResponse.Availability availability;
    @JsonProperty("related_products") private List<AdminProductSummaryResponse> relatedProducts;
    @JsonProperty("created_at") private OffsetDateTime createdAt;
    @JsonProperty("updated_at") private OffsetDateTime updatedAt;

    public static AdminProductDetailResponse from(Product product, List<ProductImage> images,
            Inventory inventory, List<AdminProductSummaryResponse> relatedProducts) {
        AdminProductDetailResponse dto = new AdminProductDetailResponse();
        dto.id = product.getId(); dto.name = product.getName(); dto.description = product.getDescription();
        dto.price = product.getPrice(); dto.productType = product.getProductType().name();
        dto.status = product.getStatus().name(); dto.categoryId = product.getCategory().getId();
        dto.categoryName = product.getCategory().getName();
        dto.images = images.stream().map(ProductImageResponse::from).toList();
        dto.availability = ProductDetailResponse.Availability.from(inventory);
        dto.relatedProducts = relatedProducts; dto.createdAt = product.getCreatedAt(); dto.updatedAt = product.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getProductType() { return productType; }
    public String getStatus() { return status; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public List<ProductImageResponse> getImages() { return images; }
    public ProductDetailResponse.Availability getAvailability() { return availability; }
    public List<AdminProductSummaryResponse> getRelatedProducts() { return relatedProducts; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
