package com.handmadeart.ecommerce.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a product to the authenticated customer's cart.
 *
 * REST API Spec §8 "Add cart item":
 *   POST /api/v1/cart/items
 *   Request: AddCartItemRequest {productId, quantity}
 *
 * Quantity must be a positive integer (FR-CART-01, REST API Spec §18).
 * Client-supplied prices or totals are never accepted.
 */
public class AddCartItemRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    public AddCartItemRequest() {
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
