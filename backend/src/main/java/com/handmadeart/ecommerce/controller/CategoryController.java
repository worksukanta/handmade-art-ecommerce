package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.service.CatalogueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public category catalogue controller.
 *
 * Endpoints (REST API Spec §6):
 *   GET /api/v1/categories       — list ACTIVE categories (public)
 *   GET /api/v1/categories/{id}  — single ACTIVE category (public, 404 if inactive)
 *
 * Both endpoints are permit-all in SecurityConfig — no authentication required.
 * Controllers are thin; all visibility logic is in CatalogueService.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogueService catalogueService;

    public CategoryController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * List all ACTIVE categories.
     *
     * Method:  GET
     * Path:    /api/v1/categories
     * Auth:    Public
     * Success: 200 OK + CategoryResponse[]
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listActiveCategories() {
        return ResponseEntity.ok(catalogueService.listActiveCategories());
    }

    /**
     * Get a single ACTIVE category.
     *
     * Method:  GET
     * Path:    /api/v1/categories/{id}
     * Auth:    Public
     * Success: 200 OK + CategoryResponse
     * Errors:  404 if category does not exist or is INACTIVE
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueService.getActiveCategory(id));
    }
}
