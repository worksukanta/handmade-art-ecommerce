package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.service.CatalogueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public product catalogue controller.
 *
 * Endpoints (REST API Spec §7):
 *   GET /api/v1/products                        — list/search/filter/sort customer-visible products (public)
 *   GET /api/v1/products/{id}                   — product detail (public, 404 if not ACTIVE)
 *   GET /api/v1/products/{id}/related-products  — related products for a product (public)
 *
 * All endpoints are permit-all in SecurityConfig — no authentication required.
 * Controllers are thin; all visibility and business logic is in CatalogueService.
 *
 * Note: GET /api/v1/admin/products (admin product listing) is a separate endpoint
 * implemented in Phase 3B.2 — it must not be confused with this public listing.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogueService catalogueService;

    public ProductController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * List customer-visible products with optional filtering, sorting, and pagination.
     *
     * Method:  GET
     * Path:    /api/v1/products
     * Auth:    Public
     * Query params (all optional):
     *   q          — free-text search (name/description)
     *   categoryId — filter by category
     *   minPrice   — minimum price (inclusive)
     *   maxPrice   — maximum price (inclusive)
     *   sort       — sort field: name | price | created_at  (default: created_at)
     *   direction  — ASC | DESC  (default: DESC for created_at, ASC for named fields)
     *   page       — zero-based page index (default: 0)
     *   size       — page size (default: 20, max: 100)
     * Success: 200 OK + PageResponse<ProductSummaryResponse>
     * Errors:  400 for invalid sort field
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> listProducts(
            @RequestParam(required = false)                     String q,
            @RequestParam(required = false)                     Long categoryId,
            @RequestParam(required = false)                     BigDecimal minPrice,
            @RequestParam(required = false)                     BigDecimal maxPrice,
            @RequestParam(required = false)                     String sort,
            @RequestParam(required = false)                     String direction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        return ResponseEntity.ok(
                catalogueService.listProducts(q, categoryId, minPrice, maxPrice,
                        sort, direction, page, size));
    }

    /**
     * Get full detail for a single ACTIVE product.
     *
     * Method:  GET
     * Path:    /api/v1/products/{id}
     * Auth:    Public
     * Success: 200 OK + ProductDetailResponse
     * Errors:  404 if product does not exist or is not ACTIVE
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueService.getProductDetail(id));
    }

    /**
     * List related products for an ACTIVE product.
     *
     * Method:  GET
     * Path:    /api/v1/products/{id}/related-products
     * Auth:    Public
     * Success: 200 OK + ProductSummaryResponse[]
     * Errors:  404 if the source product does not exist or is not ACTIVE
     */
    @GetMapping("/{id}/related-products")
    public ResponseEntity<List<ProductSummaryResponse>> getRelatedProducts(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueService.getRelatedProducts(id));
    }
}
