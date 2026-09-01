package com.handmadeart.ecommerce.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the quantity of an existing cart item.
 *
 * REST API Spec §8 "Update cart item quantity":
 *   PUT /api/v1/cart/items/{itemId}
 *   Request: UpdateCartItemRequest {quantity}
 *
 * Quantity must be a positive integer — quantity 0 is not interpreted as deletion
 * (REST API Spec §8, FR-CART-02).
 */
public class UpdateCartItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    public UpdateCartItemRequest() {
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
