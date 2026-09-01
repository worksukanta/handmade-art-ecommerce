package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.cart.AddCartItemRequest;
import com.handmadeart.ecommerce.dto.cart.CartResponse;
import com.handmadeart.ecommerce.dto.cart.UpdateCartItemRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer cart controller.
 *
 * Endpoints (REST API Spec §8):
 *   GET    /api/v1/cart               — get current cart (200 + CartResponse)
 *   POST   /api/v1/cart/items         — add item to cart (200/201 + CartResponse)
 *   PUT    /api/v1/cart/items/{itemId}— update item quantity (200 + CartResponse)
 *   DELETE /api/v1/cart/items/{itemId}— remove single item (200 + CartResponse)
 *   DELETE /api/v1/cart/items         — clear all cart items (204 No Content)
 *
 * Authorization:
 *   All endpoints require an authenticated CUSTOMER.
 *   SecurityConfig covers "/api/v1/cart" and "/api/v1/cart/**" as authenticated.
 *   Ownership is resolved from the JWT via CurrentUserService; no client-supplied
 *   user IDs are trusted.
 *
 * Controllers are thin: identity resolution and all business logic live in CartService.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    /**
     * Get current cart.
     *
     * Method:  GET
     * Path:    /api/v1/cart
     * Auth:    CUSTOMER (authenticated)
     * Success: 200 OK + CartResponse
     * Errors:  401 unauthenticated, 403 wrong role
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CartResponse response = cartService.getCart(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Add an item to the cart.
     *
     * Method:  POST
     * Path:    /api/v1/cart/items
     * Auth:    CUSTOMER (authenticated)
     * Request: AddCartItemRequest {productId, quantity}
     * Success: 200 OK + CartResponse  (item already existed → quantity accumulated)
     *          201 Created + CartResponse (new item added)
     *          The spec permits "200 OK/201 Created" — we return 200 in both cases
     *          for simplicity since the spec says "200 OK/201 Created" with either valid.
     * Errors:  400 invalid quantity/type, 401, 403, 404 product, 409 unavailable/stock rule
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CartResponse response = cartService.addItem(currentUser, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the quantity of a cart item.
     *
     * Method:  PUT
     * Path:    /api/v1/cart/items/{itemId}
     * Auth:    CUSTOMER (authenticated)
     * Request: UpdateCartItemRequest {quantity}
     * Success: 200 OK + CartResponse
     * Errors:  400 invalid quantity, 401, 403, 404 item not found/not owned, 409 insufficient stock
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CartResponse response = cartService.updateItemQuantity(currentUser, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a single item from the cart.
     *
     * Method:  DELETE
     * Path:    /api/v1/cart/items/{itemId}
     * Auth:    CUSTOMER (authenticated)
     * Success: 200 OK + CartResponse  (spec allows 200 OR 204; returning 200 with updated cart)
     * Errors:  401, 403, 404 item not found/not owned
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CartResponse response = cartService.removeItem(currentUser, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from the cart.
     *
     * Method:  DELETE
     * Path:    /api/v1/cart/items
     * Auth:    CUSTOMER (authenticated)
     * Success: 204 No Content
     * Errors:  401, 403
     */
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart() {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        cartService.clearCart(currentUser);
        return ResponseEntity.noContent().build();
    }
}
