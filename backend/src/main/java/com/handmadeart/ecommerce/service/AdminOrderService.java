package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderSummaryResponse;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for admin order management.
 *
 * REST API Spec §12:
 *   GET    /api/v1/admin/orders          — paginated order list (all customers)
 *   GET    /api/v1/admin/orders/{id}     — order detail
 *   PATCH  /api/v1/admin/orders/{id}/status — order status transition
 *
 * Order status transition rules (SRS §8.1, ERD §15.1):
 *   Approved forward lifecycle: PENDING_PAYMENT → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *   CANCELLED: DEC-006 OPEN — cancellation eligibility rules not yet decided.
 *     Admin may not transition any order to CANCELLED through this endpoint.
 *   Backwards transitions are not permitted.
 *   Skipping steps is not permitted (e.g., PENDING_PAYMENT → SHIPPED is rejected).
 *
 * Historical snapshot values in OrderItem are never recalculated from current catalogue data.
 */
@Service
public class AdminOrderService {

    /**
     * Approved forward-only transition map.
     * DEC-006 OPEN: CANCELLED is excluded — admin cannot set it via this endpoint.
     */
    private static final java.util.Map<OrderStatus, OrderStatus> ALLOWED_TRANSITIONS =
            java.util.Map.of(
                    OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED,
                    OrderStatus.CONFIRMED,       OrderStatus.PROCESSING,
                    OrderStatus.PROCESSING,      OrderStatus.SHIPPED,
                    OrderStatus.SHIPPED,         OrderStatus.DELIVERED
            );

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminOrderService(CustomerOrderRepository customerOrderRepository,
                             OrderItemRepository orderItemRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // =========================================================================
    // GET /api/v1/admin/orders — paginated order list
    // =========================================================================

    /**
     * Return all orders across all customers, paginated.
     *
     * Default sort: most recent first (createdAt DESC).
     * No status filtering at this layer — returns all orders.
     *
     * @param page zero-based page index
     * @param size page size (default 20)
     * @return paginated admin order summaries
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> listAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CustomerOrder> orderPage = customerOrderRepository.findAll(pageable);
        Page<AdminOrderSummaryResponse> summaryPage = orderPage.map(AdminOrderSummaryResponse::from);
        return PageResponse.from(summaryPage);
    }

    // =========================================================================
    // GET /api/v1/admin/orders/{id} — order detail
    // =========================================================================

    /**
     * Return full order detail for any order (admin access, not ownership-scoped).
     *
     * @param orderId path variable
     * @return full admin order detail with items
     */
    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderDetail(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return AdminOrderResponse.from(order, items);
    }

    // =========================================================================
    // PATCH /api/v1/admin/orders/{id}/status — status transition
    // =========================================================================

    /**
     * Transition an order to the next approved status.
     *
     * Only forward single-step transitions are permitted:
     *   PENDING_PAYMENT → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     *
     * DEC-006 OPEN: CANCELLED is not a valid target — returns 409.
     * Backwards/skipping transitions → 409 INVALID_TRANSITION.
     *
     * @param orderId       path variable
     * @param targetStatus  the requested new status
     * @return updated admin order response
     */
    @Transactional
    public AdminOrderResponse updateOrderStatus(Long orderId, OrderStatus targetStatus) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus current = order.getStatus();

        // DEC-006 guard: CANCELLED cannot be set via admin endpoint
        if (targetStatus == OrderStatus.CANCELLED) {
            throw new InvalidWorkflowTransitionException(
                    "Order cancellation is not yet supported (DEC-006 OPEN). "
                    + "Status transition to CANCELLED is not permitted.");
        }

        // Validate single-step forward transition
        OrderStatus allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || allowed != targetStatus) {
            throw new InvalidWorkflowTransitionException(
                    "Invalid order status transition: " + current + " → " + targetStatus
                    + ". Allowed next status: " + (allowed != null ? allowed : "none (terminal state)"));
        }

        order.setStatus(targetStatus);
        CustomerOrder saved = customerOrderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(saved.getId());
        return AdminOrderResponse.from(saved, items);
    }
}
