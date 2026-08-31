package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
