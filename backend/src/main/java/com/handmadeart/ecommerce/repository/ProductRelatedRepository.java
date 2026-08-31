package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.ProductRelated;
import com.handmadeart.ecommerce.entity.ProductRelatedId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ProductRelated}.
 *
 * Methods correspond to approved related-product operations (FR-CAT-07).
 */
public interface ProductRelatedRepository extends JpaRepository<ProductRelated, ProductRelatedId> {

    /**
     * Find all related products curated for a given source product —
     * product-detail page related-products section (FR-CAT-07).
     */
    List<ProductRelated> findByProductId(Long productId);

    /**
     * Find all pairs where the given product appears as the related side —
     * useful for symmetric display or cleanup when a product is removed.
     */
    List<ProductRelated> findByRelatedProductId(Long relatedProductId);

    /**
     * Check whether a specific directional pair already exists — prevents
     * duplicate entries when adding a related-product relationship.
     */
    boolean existsById(ProductRelatedId id);
}
