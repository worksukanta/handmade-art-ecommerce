package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CartItem}.
 *
 * Approved operations: FR-CART-01..05, UC-006.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find all items in the specified cart — used to display the cart contents.
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Find a specific item in a cart by product — used for quantity update or
     * duplicate-check before adding.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Find a specific cart item scoped to a given cart.
     * Used for ownership-safe retrieval of update/remove targets: the query is
     * scoped through the authenticated user's cartId so a foreign item cannot
     * be reached even before the application-level ownership check runs.
     */
    Optional<CartItem> findByCartIdAndId(Long cartId, Long id);

    /**
     * Count the number of distinct line items in the specified cart.
     */
    long countByCartId(Long cartId);

    /**
     * Remove all items from a cart — used when the cart is cleared or converted
     * to an order at checkout.
     */
    void deleteByCartId(Long cartId);
}
