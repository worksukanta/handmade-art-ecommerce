package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.admin.CategoryRequest;
import com.handmadeart.ecommerce.dto.admin.CategoryStatusRequest;
import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin REST controller for category management.
 *
 * Endpoints (REST API Spec §6):
 *   POST  /api/v1/admin/categories           — create category (ADMIN)
 *   PUT   /api/v1/admin/categories/{id}      — update category (ADMIN)
 *   PATCH /api/v1/admin/categories/{id}/status — change status (ADMIN)
 *
 * Authorization is enforced by SecurityConfig (all /api/v1/admin/** → ADMIN role).
 * Controllers are thin — no business logic here.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final AdminCatalogueService adminCatalogueService;

    public AdminCategoryController(AdminCatalogueService adminCatalogueService) {
        this.adminCatalogueService = adminCatalogueService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(adminCatalogueService.listAllCategories());
    }

    /**
     * Create a new category.
     *
     * Method:  POST
     * Path:    /api/v1/admin/categories
     * Auth:    ADMIN
     * Success: 201 Created + CategoryResponse
     * Errors:  400 validation, 401, 403, 409 duplicate name
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = adminCatalogueService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing category's name and/or description.
     *
     * Method:  PUT
     * Path:    /api/v1/admin/categories/{id}
     * Auth:    ADMIN
     * Success: 200 OK + CategoryResponse
     * Errors:  400, 401, 403, 404 not found, 409 duplicate name
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = adminCatalogueService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate or deactivate a category.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/categories/{id}/status
     * Auth:    ADMIN
     * Success: 200 OK + CategoryResponse
     * Errors:  400, 401, 403, 404, 409 if integrity prevents transition
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> changeCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request) {
        CategoryResponse response = adminCatalogueService.changeCategoryStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
