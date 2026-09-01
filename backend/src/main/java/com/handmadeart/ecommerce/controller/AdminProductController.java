package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.admin.InventoryResponse;
import com.handmadeart.ecommerce.dto.admin.InventoryUpdateRequest;
import com.handmadeart.ecommerce.dto.admin.ProductRequest;
import com.handmadeart.ecommerce.dto.admin.ProductStatusRequest;
import com.handmadeart.ecommerce.dto.admin.RelatedProductsRequest;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin REST controller for product, image, related-product, and inventory management.
 *
 * Endpoints (REST API Spec §7, §12, §16):
 *   GET    /api/v1/admin/products                          — admin product listing (ADMIN)
 *   POST   /api/v1/admin/products                          — create product (ADMIN)
 *   PUT    /api/v1/admin/products/{id}                     — update product (ADMIN)
 *   PATCH  /api/v1/admin/products/{id}/status              — change status (ADMIN)
 *   POST   /api/v1/admin/products/{id}/images              — upload image (ADMIN)
 *   DELETE /api/v1/admin/products/{id}/images/{imageId}    — remove image (ADMIN)
 *   PUT    /api/v1/admin/products/{id}/related-products    — set related products (ADMIN)
 *   GET    /api/v1/admin/inventory                         — list inventory (ADMIN)
 *   GET    /api/v1/admin/inventory/{productId}             — get product inventory (ADMIN)
 *   PATCH  /api/v1/admin/inventory/{productId}             — update stock (ADMIN)
 *
 * Authorization is enforced by SecurityConfig (all /api/v1/admin/** → ADMIN role).
 * Controllers are thin — no business logic here.
 */
@RestController
public class AdminProductController {

    private final AdminCatalogueService adminCatalogueService;

    public AdminProductController(AdminCatalogueService adminCatalogueService) {
        this.adminCatalogueService = adminCatalogueService;
    }

    // =========================================================================
    // Product endpoints
    // =========================================================================

    /**
     * Admin product listing — all products regardless of status.
     *
     * Method:  GET
     * Path:    /api/v1/admin/products
     * Auth:    ADMIN
     * Success: 200 OK + PageResponse<ProductSummaryResponse>
     * Errors:  401, 403
     */
    @GetMapping("/api/v1/admin/products")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> listAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminCatalogueService.listAllProducts(page, size));
    }

    /**
     * Create a new product.
     *
     * Method:  POST
     * Path:    /api/v1/admin/products
     * Auth:    ADMIN
     * Success: 201 Created + ProductDetailResponse
     * Errors:  400, 401, 403, 404 category, 409
     */
    @PostMapping("/api/v1/admin/products")
    public ResponseEntity<ProductDetailResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductDetailResponse response = adminCatalogueService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update core product data.
     *
     * Method:  PUT
     * Path:    /api/v1/admin/products/{id}
     * Auth:    ADMIN
     * Success: 200 OK + ProductDetailResponse
     * Errors:  400, 401, 403, 404
     */
    @PutMapping("/api/v1/admin/products/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductDetailResponse response = adminCatalogueService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Change product active/inactive status.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/products/{id}/status
     * Auth:    ADMIN
     * Success: 200 OK + ProductDetailResponse
     * Errors:  400, 401, 403, 404, 409
     */
    @PatchMapping("/api/v1/admin/products/{id}/status")
    public ResponseEntity<ProductDetailResponse> changeProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request) {
        ProductDetailResponse response = adminCatalogueService.changeProductStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Product image endpoints
    // =========================================================================

    /**
     * Upload a product image.
     *
     * Method:  POST
     * Path:    /api/v1/admin/products/{id}/images
     * Auth:    ADMIN
     * Request: multipart/form-data, field name "file"
     * Success: 201 Created + ProductImageResponse
     * Errors:  400 invalid file, 401, 403, 404
     *
     * DEC-003 (file type/size limits) is OPEN.
     * File type validated (image/* only); size limit not enforced (DEC-003 pending).
     */
    @PostMapping(value = "/api/v1/admin/products/{id}/images",
                 consumes = "multipart/form-data")
    public ResponseEntity<ProductImageResponse> addProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ProductImageResponse response = adminCatalogueService.addProductImage(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove a product image.
     *
     * Method:  DELETE
     * Path:    /api/v1/admin/products/{id}/images/{imageId}
     * Auth:    ADMIN
     * Success: 204 No Content
     * Errors:  401, 403, 404
     */
    @DeleteMapping("/api/v1/admin/products/{id}/images/{imageId}")
    public ResponseEntity<Void> removeProductImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        adminCatalogueService.removeProductImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Related products endpoint
    // =========================================================================

    /**
     * Replace the related-product set for a product (full replacement).
     *
     * Method:  PUT
     * Path:    /api/v1/admin/products/{id}/related-products
     * Auth:    ADMIN
     * Success: 200 OK + ProductSummaryResponse[]
     * Errors:  400 self-reference/invalid IDs, 401, 403, 404
     */
    @PutMapping("/api/v1/admin/products/{id}/related-products")
    public ResponseEntity<List<ProductSummaryResponse>> replaceRelatedProducts(
            @PathVariable Long id,
            @Valid @RequestBody RelatedProductsRequest request) {
        List<ProductSummaryResponse> response = adminCatalogueService.replaceRelatedProducts(id, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Inventory endpoints
    // =========================================================================

    /**
     * Admin inventory listing.
     *
     * Method:  GET
     * Path:    /api/v1/admin/inventory
     * Auth:    ADMIN
     * Success: 200 OK + PageResponse<InventoryResponse>
     * Errors:  401, 403
     */
    @GetMapping("/api/v1/admin/inventory")
    public ResponseEntity<PageResponse<InventoryResponse>> listInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminCatalogueService.listInventory(page, size));
    }

    /**
     * Get inventory for a specific product.
     *
     * Method:  GET
     * Path:    /api/v1/admin/inventory/{productId}
     * Auth:    ADMIN
     * Success: 200 OK + InventoryResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/api/v1/admin/inventory/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(adminCatalogueService.getInventory(productId));
    }

    /**
     * Update stock for a product.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/inventory/{productId}
     * Auth:    ADMIN
     * Success: 200 OK + InventoryResponse
     * Errors:  400 negative/invalid, 401, 403, 404, 409 product type conflict
     *
     * DEC-009 (inventory concurrency strategy) remains OPEN.
     */
    @PatchMapping("/api/v1/admin/inventory/{productId}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(adminCatalogueService.updateInventory(productId, request));
    }
}
