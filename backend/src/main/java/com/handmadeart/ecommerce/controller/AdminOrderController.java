package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderStatusRequest;
import com.handmadeart.ecommerce.dto.order.AdminOrderSummaryResponse;
import com.handmadeart.ecommerce.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin order management controller.
 *
 * Endpoints (REST API Spec §12):
 *   GET   /api/v1/admin/orders          — paginated list of all orders
 *   GET   /api/v1/admin/orders/{id}     — order detail
 *   PATCH /api/v1/admin/orders/{id}/status — order status transition
 *
 * Authorization: ADMIN role required (SecurityConfig — all /api/v1/admin/**).
 * This controller is thin: all business logic is in AdminOrderService.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /**
     * List all orders across all customers, paginated.
     *
     * Method:  GET
     * Path:    /api/v1/admin/orders
     * Auth:    ADMIN
     * Params:  page (default 0), size (default 20)
     * Success: 200 OK + PageResponse&lt;AdminOrderSummaryResponse&gt;
     * Errors:  401, 403
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminOrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminOrderService.listAllOrders(page, size));
    }

    /**
     * Get full detail for a single order.
     *
     * Method:  GET
     * Path:    /api/v1/admin/orders/{id}
     * Auth:    ADMIN
     * Success: 200 OK + AdminOrderResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(id));
    }

    /**
     * Transition an order's status.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/orders/{id}/status
     * Auth:    ADMIN
     * Request: AdminOrderStatusRequest {status}
     * Success: 200 OK + AdminOrderResponse
     * Errors:  400 validation, 401, 403, 404, 409 invalid transition
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(id, request.getStatus()));
    }
}
