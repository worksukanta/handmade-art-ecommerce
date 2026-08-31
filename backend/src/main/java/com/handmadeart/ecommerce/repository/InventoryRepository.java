package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Inventory}.
 *
 * product_id is both the PK and the FK, so the standard findById(productId)
 * from JpaRepository serves as the primary lookup.
 *
 * Methods correspond to approved inventory operations (FR-INV-01..04, ADM-02).
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Find the inventory row for a product — primary stock lookup.
     * Equivalent to findById(productId) but named explicitly for readability.
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Check whether an inventory record exists for a product.
     * Used when creating inventory for a new product.
     */
    boolean existsByProductId(Long productId);
}
