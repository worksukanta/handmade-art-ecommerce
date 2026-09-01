package com.handmadeart.ecommerce.dto.catalogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full product representation for the product-detail endpoint.
 *
 * Returned by:
 *   GET /api/v1/products/{id}
 *
 * Approved fields (REST API Spec §7, §17, ProductDetailResponse):
 *   id, name, description, price, product_type, category (id + name),
 *   images[], availability (in_stock, quantity_on_hand where applicable),
 *   related_products[], created_at, updated_at.
 *
 * Internal implementation fields are not exposed.
 * availability.quantity_on_hand is null for PORTFOLIO_ONLY products (no inventory row).
 */
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    private List<ProductImageResponse> images;

    private Availability availability;

    @JsonProperty("related_products")
    private List<ProductSummaryResponse> relatedProducts;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public ProductDetailResponse() {
    }

    public static ProductDetailResponse from(Product product,
                                             List<ProductImage> images,
                                             Inventory inventory,
                                             List<ProductSummaryResponse> relatedProducts) {
        ProductDetailResponse dto = new ProductDetailResponse();
        dto.id = product.getId();
        dto.name = product.getName();
        dto.description = product.getDescription();
        dto.price = product.getPrice();
        dto.productType = product.getProductType().name();
        dto.categoryId = product.getCategory().getId();
        dto.categoryName = product.getCategory().getName();
        dto.images = images.stream().map(ProductImageResponse::from).toList();
        dto.availability = Availability.from(inventory);
        dto.relatedProducts = relatedProducts;
        dto.createdAt = product.getCreatedAt();
        dto.updatedAt = product.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getProductType() { return productType; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public List<ProductImageResponse> getImages() { return images; }
    public Availability getAvailability() { return availability; }
    public List<ProductSummaryResponse> getRelatedProducts() { return relatedProducts; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Customer-facing availability summary.
     * {@code quantity_on_hand} is null for PORTFOLIO_ONLY products (no inventory row).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Availability {

        @JsonProperty("in_stock")
        private boolean inStock;

        @JsonProperty("quantity_on_hand")
        private Integer quantityOnHand;

        private Availability() {
        }

        public static Availability from(Inventory inventory) {
            Availability a = new Availability();
            if (inventory != null) {
                a.quantityOnHand = inventory.getQuantityOnHand();
                a.inStock = inventory.getQuantityOnHand() > 0;
            } else {
                // PORTFOLIO_ONLY — no inventory row
                a.inStock = false;
                a.quantityOnHand = null;
            }
            return a;
        }

        public boolean isInStock() { return inStock; }
        public Integer getQuantityOnHand() { return quantityOnHand; }
    }
}
