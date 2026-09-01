package com.handmadeart.ecommerce.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for replacing the related-product set for a product.
 *
 * Used by:
 *   PUT /api/v1/admin/products/{id}/related-products
 *
 * Approved field (REST API Spec §7, tbl[27] RelatedProductsRequest):
 *   productIds — list of product IDs to become the new related set.
 *                An empty list clears all related products.
 *                Self-reference (sourceId == any productId) is rejected (400).
 */
public class RelatedProductsRequest {

    @NotNull(message = "productIds list is required")
    private List<Long> productIds;

    public RelatedProductsRequest() {
    }

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
}
