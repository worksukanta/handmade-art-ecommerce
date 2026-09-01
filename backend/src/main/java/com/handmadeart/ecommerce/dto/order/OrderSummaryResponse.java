package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Lightweight order summary used in the paginated order-history list.
 *
 * REST API Spec §10 "List my orders":
 *   "200 OK + PageResponse&lt;OrderSummaryResponse&gt;"
 *   DTO table: "order id, item snapshots, totals, address snapshot/reference, status, timestamps"
 *
 * Returns a subset of the full OrderResponse for list performance:
 * orderId, status, shipping address city/country, subtotal, total, createdAt, updatedAt.
 * Full item details are available via GET /orders/{id}.
 */
public class OrderSummaryResponse {

    private Long orderId;
    private OrderStatus status;

    // Minimal shipping address info for list display
    private String shipRecipientName;
    private String shipCity;
    private String shipCountry;

    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public OrderSummaryResponse() {
    }

    /**
     * Build an OrderSummaryResponse from a persisted {@link CustomerOrder}.
     * Item details are excluded — use {@link OrderResponse} for detail view.
     */
    public static OrderSummaryResponse from(CustomerOrder order) {
        OrderSummaryResponse dto = new OrderSummaryResponse();
        dto.orderId = order.getId();
        dto.status = order.getStatus();
        dto.shipRecipientName = order.getShipRecipientName();
        dto.shipCity = order.getShipCity();
        dto.shipCountry = order.getShipCountry();
        dto.subtotalAmount = order.getSubtotalAmount();
        dto.totalAmount = order.getTotalAmount();
        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public String getShipRecipientName() { return shipRecipientName; }
    public String getShipCity() { return shipCity; }
    public String getShipCountry() { return shipCountry; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
