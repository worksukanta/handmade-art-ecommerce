package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-view order detail response.
 *
 * Extends the standard order shape with the owning customer's id and email
 * so admins can identify whose order they are managing.
 *
 * REST API Spec §12 (Admin order management):
 *   GET /api/v1/admin/orders/{id} — order detail
 *   PATCH /api/v1/admin/orders/{id}/status — transition result
 *
 * Historical snapshot values (item names, unit prices) are never recalculated
 * from the current catalogue — they are read from stored OrderItem rows (BR-11).
 */
public class AdminOrderResponse {

    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private OrderStatus status;

    private String shipRecipientName;
    private String shipLine1;
    private String shipLine2;
    private String shipCity;
    private String shipStateProvince;
    private String shipPostalCode;
    private String shipCountry;
    private String shipPhone;

    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;

    private List<OrderItemResponse> items;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AdminOrderResponse() {
    }

    public static AdminOrderResponse from(CustomerOrder order, List<OrderItem> items) {
        AdminOrderResponse dto = new AdminOrderResponse();
        dto.orderId = order.getId();
        dto.customerId = order.getUser().getId();
        dto.customerEmail = order.getUser().getEmail();
        dto.status = order.getStatus();

        dto.shipRecipientName = order.getShipRecipientName();
        dto.shipLine1 = order.getShipLine1();
        dto.shipLine2 = order.getShipLine2();
        dto.shipCity = order.getShipCity();
        dto.shipStateProvince = order.getShipStateProvince();
        dto.shipPostalCode = order.getShipPostalCode();
        dto.shipCountry = order.getShipCountry();
        dto.shipPhone = order.getShipPhone();

        dto.subtotalAmount = order.getSubtotalAmount();
        dto.totalAmount = order.getTotalAmount();

        dto.items = items.stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        return dto;
    }

    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public String getShipRecipientName() { return shipRecipientName; }
    public String getShipLine1() { return shipLine1; }
    public String getShipLine2() { return shipLine2; }
    public String getShipCity() { return shipCity; }
    public String getShipStateProvince() { return shipStateProvince; }
    public String getShipPostalCode() { return shipPostalCode; }
    public String getShipCountry() { return shipCountry; }
    public String getShipPhone() { return shipPhone; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItemResponse> getItems() { return items; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
