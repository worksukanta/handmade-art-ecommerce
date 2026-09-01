package com.handmadeart.ecommerce.dto.cart;

import com.handmadeart.ecommerce.entity.CartItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for a single cart line item.
 *
 * CartResponse.items is a list of CartItemResponse records.
 *
 * CartItem intentionally has no persisted price (Database Design §8.3, §6.1).
 * The unit_price and subtotal here are computed from the current authoritative
 * product price at response time — they are never taken from client input.
 *
 * Fields returned (REST API Spec §17 CartResponse "cart id, items, server-calculated subtotal/total"):
 *   itemId        — cart_item.id
 *   productId     — product.id
 *   productName   — product.name
 *   unitPrice     — current product.price (server-authoritative)
 *   quantity      — cart_item.quantity
 *   subtotal      — unitPrice × quantity (server-calculated)
 *   addedAt       — cart_item.added_at
 */
public class CartItemResponse {

    private Long itemId;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private OffsetDateTime addedAt;

    public CartItemResponse() {
    }

    /**
     * Build a CartItemResponse from a CartItem entity.
     * The unit price is taken from the associated Product — backend-authoritative.
     */
    public static CartItemResponse from(CartItem item) {
        CartItemResponse dto = new CartItemResponse();
        dto.itemId = item.getId();
        dto.productId = item.getProduct().getId();
        dto.productName = item.getProduct().getName();
        dto.unitPrice = item.getProduct().getPrice();
        dto.quantity = item.getQuantity();
        dto.subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        dto.addedAt = item.getAddedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters (read-only response DTO)
    // -------------------------------------------------------------------------

    public Long getItemId() { return itemId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public OffsetDateTime getAddedAt() { return addedAt; }
}
