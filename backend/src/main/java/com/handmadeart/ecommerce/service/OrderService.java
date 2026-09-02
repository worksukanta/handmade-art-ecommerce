package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.dto.order.OrderSummaryResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for customer order read operations.
 *
 * REST API Spec §10:
 *   GET /api/v1/orders        — paginated order history for the authenticated customer
 *   GET /api/v1/orders/{id}   — single order detail for the authenticated customer
 *
 * Ownership rule (BR-06, REST API Spec §10):
 *   All lookups are scoped to the authenticated customer's user ID.
 *   A foreign orderId returns 404 (non-disclosure — indistinguishable from "not found").
 *   The authenticated user is always resolved via {@link CurrentUserService};
 *   no client-supplied user ID is ever trusted.
 *
 * Read operations: no state mutation, no inventory changes.
 * Returns immutable purchase-time snapshot values from OrderItem — never live product data.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;

    public OrderService(CustomerOrderRepository customerOrderRepository,
                        OrderItemRepository orderItemRepository,
                        ShipmentRepository shipmentRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
    }

    // =========================================================================
    // GET /api/v1/orders — paginated order history
    // =========================================================================

    /**
     * Return the authenticated customer's order history, paginated.
     *
     * REST API Spec §10: "page, size, sort and approved status filtering where implemented"
     * Default sort: most recent first (createdAt DESC).
     *
     * @param currentUser authenticated customer
     * @param page        zero-based page index
     * @param size        page size (default 20)
     * @return paginated list of order summaries
     */
    public PageResponse<OrderSummaryResponse> getOrderHistory(AppUser currentUser,
                                                              int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomerOrder> orderPage = customerOrderRepository
                .findByUserId(currentUser.getId(), pageable);

        Page<OrderSummaryResponse> summaryPage = orderPage.map(OrderSummaryResponse::from);
        return PageResponse.from(summaryPage);
    }

    // =========================================================================
    // GET /api/v1/orders/{id} — single order detail
    // =========================================================================

    /**
     * Return a single order owned by the authenticated customer.
     *
     * Ownership is enforced via {@code findByUserIdAndId}: a foreign orderId
     * is indistinguishable from a missing orderId (404, non-disclosure).
     *
     * Item snapshot values (productNameSnapshot, unitPriceSnapshot, lineTotal)
     * are read directly from the stored OrderItem rows — not recalculated from
     * the live product catalogue (FR-ORD-03, BR-11).
     *
     * @param currentUser authenticated customer
     * @param orderId     path variable
     * @return full order detail
     */
    public OrderResponse getOrderDetail(AppUser currentUser, Long orderId) {
        CustomerOrder order = customerOrderRepository
                .findByUserIdAndId(currentUser.getId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderResponse.from(order, items);
    }

    // =========================================================================
    // GET /api/v1/orders/{id}/shipment — customer view order shipment
    // =========================================================================

    /**
     * Return the shipment for an order owned by the authenticated customer.
     *
     * Ownership is enforced: foreign orderId → 404 (same non-disclosure semantics).
     * If no shipment exists yet for the order, returns 404.
     * No carrier API integration (DEC-008 APPROVED).
     *
     * @param currentUser authenticated customer
     * @param orderId     path variable; ownership verified
     * @return ShipmentResponse for the order's shipment
     */
    public ShipmentResponse getOrderShipment(AppUser currentUser, Long orderId) {
        // Verify order ownership first (non-disclosure: foreign orderId → 404)
        customerOrderRepository
                .findByUserIdAndId(currentUser.getId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for this order"));

        return ShipmentResponse.from(shipment);
    }
}
