package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.dto.order.OrderSummaryResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer order read controller.
 *
 * Endpoints (REST API Spec §10):
 *   GET /api/v1/orders        — paginated order history for the authenticated customer
 *   GET /api/v1/orders/{id}   — single order detail for the authenticated customer
 *
 * Authorization:
 *   CUSTOMER role required (SecurityConfig — /api/v1/orders/**).
 *   Identity resolved from JWT via CurrentUserService; no client-supplied user IDs trusted.
 *
 * This controller is thin: all business logic is in OrderService.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    /**
     * List the authenticated customer's order history.
     *
     * Method:  GET
     * Path:    /api/v1/orders
     * Auth:    CUSTOMER
     * Params:  page (default 0), size (default 20)
     * Success: 200 OK + PageResponse&lt;OrderSummaryResponse&gt;
     * Errors:  401, 403
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        PageResponse<OrderSummaryResponse> response =
                orderService.getOrderHistory(currentUser, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single order owned by the authenticated customer.
     *
     * Method:  GET
     * Path:    /api/v1/orders/{id}
     * Auth:    CUSTOMER
     * Success: 200 OK + OrderResponse
     * Errors:  401, 403, 404 (foreign or missing orderId)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        OrderResponse response = orderService.getOrderDetail(currentUser, id);
        return ResponseEntity.ok(response);
    }
}
