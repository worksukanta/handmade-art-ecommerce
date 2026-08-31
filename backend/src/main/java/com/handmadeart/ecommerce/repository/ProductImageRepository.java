package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProductImage}.
 *
 * Methods correspond to approved image management operations (FR-PROD-04, ADM-01).
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Find all images for a product ordered by display position — product gallery
     * (FR-PROD-04).
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    /**
     * Find the primary thumbnail image for a product — catalogue listing display.
     */
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    /**
     * Count images for a product — used to validate upload limits if applicable.
     */
    long countByProductId(Long productId);
}
