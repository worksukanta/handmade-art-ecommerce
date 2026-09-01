package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.OrderItem;

import java.math.BigDecimal;

/**
 * Response DTO for a single order line item.
 *
 * All price/name values come from the immutable purchase-time snapshots on
 * {@link com.handmadeart.ecommerce.entity.OrderItem}, never from the live
 * product (Database Design §9.2, FR-ORD-03, BR-11).
 */
public class OrderItemResponse {

    private Long itemId;
    private Long productId;         // nullable — product may be deleted later
    private String productName;     // snapshot
    private BigDecimal unitPrice;   // snapshot
    private Integer quantity;
    private BigDecimal lineTotal;   // snapshot = unitPrice × quantity

    public OrderItemResponse() {
    }

    public static OrderItemResponse from(OrderItem item) {
        OrderItemResponse dto = new OrderItemResponse();
        dto.itemId = item.getId();
        dto.productId = item.getProduct() != null ? item.getProduct().getId() : null;
        dto.productName = item.getProductNameSnapshot();
        dto.unitPrice = item.getUnitPriceSnapshot();
        dto.quantity = item.getQuantity();
        dto.lineTotal = item.getLineTotal();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getItemId() { return itemId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
