package com.handmadeart.ecommerce.exception;

/**
 * Thrown when a payment is attempted on an order that is not in a payable state.
 *
 * An order is payable only when its status is {@code PENDING_PAYMENT}.
 * Any other status (CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
 * means either payment has already succeeded or the order is not eligible.
 *
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class OrderNotPayableException extends RuntimeException {

    public OrderNotPayableException(String message) {
        super(message);
    }
}
