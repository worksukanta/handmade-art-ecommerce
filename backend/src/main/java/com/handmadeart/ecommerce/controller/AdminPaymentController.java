package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.order.AdminPaymentResponse;
import com.handmadeart.ecommerce.service.AdminPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin payment management controller.
 *
 * Endpoints (REST API Spec §12):
 *   GET /api/v1/admin/payments/{id} — payment detail (read-only)
 *
 * Authorization: ADMIN role required (SecurityConfig — all /api/v1/admin/**).
 *
 * Provider callback (POST /payments/provider-callback) is NOT implemented —
 * blocked by DEC-001 (payment provider selection DEFERRED).
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    /**
     * Get payment detail for any payment record.
     *
     * Method:  GET
     * Path:    /api/v1/admin/payments/{id}
     * Auth:    ADMIN
     * Success: 200 OK + AdminPaymentResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminPaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(adminPaymentService.getPayment(id));
    }
}
