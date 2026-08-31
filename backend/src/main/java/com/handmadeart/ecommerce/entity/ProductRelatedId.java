package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link ProductRelated}.
 *
 * Represents the two-column composite PK (product_id, related_product_id)
 * of the {@code product_related} junction table (ERD §3.6).
 */
@Embeddable
public class ProductRelatedId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "related_product_id", nullable = false)
    private Long relatedProductId;

    public ProductRelatedId() {
    }

    public ProductRelatedId(Long productId, Long relatedProductId) {
        this.productId = productId;
        this.relatedProductId = relatedProductId;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getRelatedProductId() { return relatedProductId; }
    public void setRelatedProductId(Long relatedProductId) { this.relatedProductId = relatedProductId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRelatedId that)) return false;
        return Objects.equals(productId, that.productId)
                && Objects.equals(relatedProductId, that.relatedProductId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, relatedProductId);
    }
}
