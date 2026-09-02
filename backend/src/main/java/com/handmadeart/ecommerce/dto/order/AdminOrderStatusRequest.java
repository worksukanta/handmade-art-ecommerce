package com.handmadeart.ecommerce.dto.order;

import com.handmadeart.ecommerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for PATCH /api/v1/admin/orders/{id}/status.
 *
 * Admin supplies the target status; the service enforces valid transitions.
 * DEC-006 OPEN: CANCELLED is defined in the enum but cannot be triggered via this
 * endpoint until cancellation eligibility rules are decided.
 */
public class AdminOrderStatusRequest {

    @NotNull(message = "status is required")
    private OrderStatus status;

    public AdminOrderStatusRequest() {
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
