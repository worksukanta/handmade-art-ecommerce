package com.handmadeart.ecommerce.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a product.
 *
 * Used by:
 *   POST /api/v1/admin/products       (create)
 *   PUT  /api/v1/admin/products/{id}  (update)
 *
 * Approved fields (REST API Spec §7, §17 ProductRequest):
 *   name        — required, 1–200 chars
 *   description — optional
 *   price       — required, >= 0 (BR-15, validation rule §18)
 *   categoryId  — required; must reference an existing active category
 *   productType — required; READY_MADE | CUSTOM_AVAILABLE | PORTFOLIO_ONLY
 *   status      — required; ACTIVE | INACTIVE
 *
 * Price cannot be negative (REST API Spec §18).
 */
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    @JsonProperty("category_id")
    private Long categoryId;

    @NotNull(message = "Product type is required")
    @JsonProperty("product_type")
    private String productType;

    @NotNull(message = "Status is required")
    private String status;

    public ProductRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
