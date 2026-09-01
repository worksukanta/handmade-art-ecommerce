package com.handmadeart.ecommerce.exception;

/**
 * Thrown when checkout/order-creation is attempted on an empty cart.
 *
 * Mapped to 409 Conflict by {@link GlobalExceptionHandler} — an empty cart
 * is not a client validation error but a business-rule conflict with the
 * checkout precondition.
 */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException(String message) {
        super(message);
    }
}
