package com.handmadeart.ecommerce.dto.cart;

import com.handmadeart.ecommerce.entity.Cart;
import com.handmadeart.ecommerce.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for the authenticated customer's cart.
 *
 * REST API Spec §17 CartResponse:
 *   "cart id, items, server-calculated subtotal/total"
 *
 * Cart totals are always server-calculated from current product prices.
 * Client-supplied totals are never accepted or trusted (REST API Spec §18).
 *
 * DEC-007 (tax/delivery charge calculation): DEFERRED.
 * No tax or delivery charge is added — only item subtotals are summed for the total.
 *
 * Fields:
 *   cartId   — cart.id
 *   items    — ordered list of CartItemResponse
 *   total    — sum of all item subtotals (server-calculated, BigDecimal)
 */
public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal total;

    public CartResponse() {
    }

    /**
     * Build a CartResponse from a Cart entity and its items.
     *
     * Items must have their Product association initialized (not a lazy proxy).
     * Total is the sum of (product.price × quantity) for each item.
     */
    public static CartResponse from(Cart cart, List<CartItem> items) {
        CartResponse dto = new CartResponse();
        dto.cartId = cart.getId();
        dto.items = items.stream()
                .map(CartItemResponse::from)
                .collect(Collectors.toList());
        dto.total = dto.items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters (read-only response DTO)
    // -------------------------------------------------------------------------

    public Long getCartId() { return cartId; }
    public List<CartItemResponse> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
}
