package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.admin.AdminCustomerResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.service.AdminCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin customer management controller (read-only).
 *
 * Endpoints (REST API Spec §16):
 *   GET /api/v1/admin/customers       — paginated customer list
 *   GET /api/v1/admin/customers/{id}  — customer detail
 *
 * Authorization: ADMIN role required (SecurityConfig — all /api/v1/admin/**).
 * Read-only: no customer mutation, deletion, or role management.
 * Password hash is never exposed.
 */
@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    /**
     * List all customers, paginated.
     *
     * Method:  GET
     * Path:    /api/v1/admin/customers
     * Auth:    ADMIN
     * Params:  page (default 0), size (default 20)
     * Success: 200 OK + PageResponse&lt;AdminCustomerResponse&gt;
     * Errors:  401, 403
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminCustomerResponse>> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminCustomerService.listCustomers(page, size));
    }

    /**
     * Get a single customer's detail.
     *
     * Method:  GET
     * Path:    /api/v1/admin/customers/{id}
     * Auth:    ADMIN
     * Success: 200 OK + AdminCustomerResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminCustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(adminCustomerService.getCustomer(id));
    }
}
