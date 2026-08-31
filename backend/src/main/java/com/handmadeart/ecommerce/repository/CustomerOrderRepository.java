package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CustomerOrder}.
 *
 * Approved operations: FR-ORD-01..09, UC-007, UC-009, UC-010, UC-018.
 */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    /**
     * Find all orders for a customer, paginated — 'My Orders' history view (UC-009).
     * Index on customer_order.user_id serves this query (ERD §17).
     */
    Page<CustomerOrder> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all orders with a given status, paginated — Admin order management
     * queue (UC-010, UC-018). Index on customer_order.status serves this query.
     */
    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders by customer and status — customer-facing filtered history.
     */
    List<CustomerOrder> findByUserIdAndStatus(Long userId, OrderStatus status);
}
