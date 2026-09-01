package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Product}.
 *
 * Methods correspond to approved catalogue and admin operations
 * (FR-PROD-01..08, FR-CAT-02/03/06, FR-INV-02, ADM-01).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products in a category with a given status — customer catalogue
     * by category (FR-CAT-02/03).
     */
    List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    /**
     * Find all products with a given status, paginated — base customer catalogue
     * (FR-PROD-01/02).
     */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * Find all products with a given status and product type — filtered catalogue
     * (FR-PROD-06, composite index on status+product_type serves this query).
     */
    List<Product> findByStatusAndProductType(ProductStatus status, ProductType productType);

    /**
     * Find all products in a category — Admin product management view.
     */
    List<Product> findByCategoryId(Long categoryId);

    /**
     * Count active products in a category — used when checking whether a category
     * can be safely deactivated.
     */
    long countByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    /**
     * Paginated catalogue search with optional filters.
     *
     * All filter parameters are optional (null means "no filter applied").
     * Status is always applied — public catalogue always passes ACTIVE.
     *
     * Approved query parameters (REST API Spec §7, §21):
     *   q          — case-insensitive substring match against name or description
     *   categoryId — restrict to a single category
     *   minPrice   — minimum price (inclusive)
     *   maxPrice   — maximum price (inclusive)
     *
     * Sorting is handled by the Pageable object, validated at service level.
     * DB-side pagination prevents loading the entire catalogue into memory.
     */
    @Query("""
            SELECT p FROM Product p
            JOIN FETCH p.category
            WHERE p.status = :status
              AND (:q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> searchCatalogue(
            @Param("status") ProductStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
