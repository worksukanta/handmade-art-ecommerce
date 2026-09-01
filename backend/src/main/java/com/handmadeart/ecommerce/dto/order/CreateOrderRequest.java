package com.handmadeart.ecommerce.dto.order;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating an order from the current cart.
 *
 * REST API Spec §9 "Create order from cart":
 *   POST /api/v1/orders
 *   Request: CreateOrderRequest {addressId}
 *
 * The cart contents are resolved server-side from the authenticated customer's
 * cart — client-supplied totals or item lists are never accepted.
 * The selected address must be owned by the authenticated customer (DEC-010
 * deferred; explicit owned address required — no silent default fallback).
 */
public class CreateOrderRequest {

    @NotNull(message = "addressId is required")
    private Long addressId;

    public CreateOrderRequest() {
    }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
}
