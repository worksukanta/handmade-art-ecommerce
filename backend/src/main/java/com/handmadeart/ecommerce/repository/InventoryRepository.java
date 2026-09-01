package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Find and pessimistically lock the inventory row for a product.
     *
     * DEC-009 APPROVED: checkout-time pessimistic locking.
     * Issues SELECT … FOR UPDATE when called inside an active transaction,
     * preventing concurrent checkouts from reading or modifying the same row
     * until the transaction commits or rolls back.
     *
     * Must only be called from within a {@code @Transactional} boundary.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * Check whether an inventory record exists for a product.
     * Used when creating inventory for a new product.
     */
    boolean existsByProductId(Long productId);
}
