package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Find a public catalogue image only when its owning product has the required status.
     */
    @Query("""
            select image
            from ProductImage image
            join fetch image.product product
            where image.id = :imageId and product.status = :status
            """)
    Optional<ProductImage> findPublicImage(
            @Param("imageId") Long imageId,
            @Param("status") ProductStatus status);
}
