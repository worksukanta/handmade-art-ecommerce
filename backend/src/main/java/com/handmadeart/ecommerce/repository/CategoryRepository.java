package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Category}.
 *
 * Methods correspond to approved operations (FR-CAT-02/03, ADM-03).
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Find all categories with a given status — used for customer navigation (ACTIVE only). */
    List<Category> findByStatus(CategoryStatus status);

    /** Find a category by exact name — used for duplicate-name checking on Admin create/update. */
    Optional<Category> findByName(String name);

    /** Check whether a category name already exists — Admin creation validation. */
    boolean existsByName(String name);
}
