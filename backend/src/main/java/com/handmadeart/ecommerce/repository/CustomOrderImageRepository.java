package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.CustomOrderImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CustomOrderImage}.
 *
 * Approved operations: FR-CUST-03, UC-011.
 *
 * Index on {@code custom_order_image.custom_order_request_id} (FK index, ERD §17)
 * supports loading all reference images for a given request.
 */
public interface CustomOrderImageRepository extends JpaRepository<CustomOrderImage, Long> {

    /**
     * Find all reference images for a given custom order request.
     * Used when displaying or reviewing request details.
     * FK index on custom_order_request_id serves this query (ERD §17).
     */
    List<CustomOrderImage> findByCustomOrderRequestId(Long customOrderRequestId);

    /**
     * Count reference images for a given custom order request.
     * Used for validation (e.g., checking at least one image exists).
     */
    long countByCustomOrderRequestId(Long customOrderRequestId);
}
