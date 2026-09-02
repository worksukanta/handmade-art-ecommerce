package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.order.CheckoutValidationResponse;
import com.handmadeart.ecommerce.dto.order.CreateOrderRequest;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.CheckoutService;
import com.handmadeart.ecommerce.service.CheckoutValidationService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer checkout controller.
 *
 * Endpoints (REST API Spec §9):
 *   POST /api/v1/checkout/validate — pre-order advisory validation (NON-MUTATING)
 *   POST /api/v1/orders            — create a ready-made order from the current cart
 *
 * Authorization:
 *   CUSTOMER role required (SecurityConfig).
 *   Identity resolved from JWT via CurrentUserService; no client-supplied user IDs trusted.
 *
 * This controller is thin: all business logic is in CheckoutService /
 * CheckoutValidationService.
 */
@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CheckoutValidationService checkoutValidationService;
    private final CurrentUserService currentUserService;

    public CheckoutController(CheckoutService checkoutService,
                               CheckoutValidationService checkoutValidationService,
                               CurrentUserService currentUserService) {
        this.checkoutService = checkoutService;
        this.checkoutValidationService = checkoutValidationService;
        this.currentUserService = currentUserService;
    }

    /**
     * Pre-order advisory validation — NON-MUTATING.
     *
     * Method:  POST
     * Path:    /api/v1/checkout/validate
     * Auth:    CUSTOMER
     * Request: CreateOrderRequest {addressId}
     * Success: 200 OK + CheckoutValidationResponse
     * Errors:  400 missing addressId, 401, 403, 404 address not owned, 409 empty cart / stock
     *
     * Does NOT create an order, decrement inventory, or modify any state.
     * Actual checkout (POST /orders) remains authoritative.
     */
    @PostMapping("/api/v1/checkout/validate")
    public ResponseEntity<CheckoutValidationResponse> validateCheckout(
            @Valid @RequestBody CreateOrderRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CheckoutValidationResponse response = checkoutValidationService.validate(currentUser, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a ready-made order from the authenticated customer's cart.
     *
     * Method:  POST
     * Path:    /api/v1/orders
     * Auth:    CUSTOMER (authenticated)
     * Request: CreateOrderRequest {addressId}
     * Success: 201 Created + OrderResponse
     * Errors:  400 missing addressId, 401, 403, 404 address not owned, 409 empty cart / stock
     */
    @PostMapping("/api/v1/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        OrderResponse response = checkoutService.createOrder(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
