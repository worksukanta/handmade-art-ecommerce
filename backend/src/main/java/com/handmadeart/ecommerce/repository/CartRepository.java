package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Cart}.
 *
 * Each customer has at most one cart (UNIQUE user_id constraint).
 * The primary lookup is by the owning user's ID.
 *
 * Approved operations: FR-CART-01..07, UC-006.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find the cart belonging to the specified customer.
     * Returns empty if the customer has not yet added anything to their cart.
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Check whether a cart already exists for the specified customer.
     * Used to decide whether to create a new cart or reuse the existing one.
     */
    boolean existsByUserId(Long userId);
}
