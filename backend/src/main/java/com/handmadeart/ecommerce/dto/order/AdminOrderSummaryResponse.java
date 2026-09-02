package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Admin-view order summary for the paginated admin order list.
 *
 * Adds customerId and customerEmail to the standard summary shape so admins
 * can see whose orders they are managing.
 *
 * REST API Spec §12: GET /api/v1/admin/orders — order processing list.
 */
public class AdminOrderSummaryResponse {

    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private OrderStatus status;
    private String shipRecipientName;
    private String shipCity;
    private String shipCountry;
    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AdminOrderSummaryResponse() {
    }

    public static AdminOrderSummaryResponse from(CustomerOrder order) {
        AdminOrderSummaryResponse dto = new AdminOrderSummaryResponse();
        dto.orderId = order.getId();
        dto.customerId = order.getUser().getId();
        dto.customerEmail = order.getUser().getEmail();
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

    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public String getShipRecipientName() { return shipRecipientName; }
    public String getShipCity() { return shipCity; }
    public String getShipCountry() { return shipCountry; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
