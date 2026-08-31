package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link OrderItem}.
 *
 * Approved operations: FR-ORD-02, FR-ORD-03, UC-007.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items for a given order — order detail view.
     * Index on order_item.order_id serves this query (ERD §17).
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Count items in an order — used for validation that an order is non-empty.
     */
    long countByOrderId(Long orderId);
}
