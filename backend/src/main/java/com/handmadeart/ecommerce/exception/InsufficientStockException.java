package com.handmadeart.ecommerce.exception;

/**
 * Thrown when a requested cart quantity exceeds the available stock for a product.
 *
 * Mapped to 409 Conflict by {@link GlobalExceptionHandler}
 * (REST API Spec §8 "Add cart item" / "Update cart item quantity" Errors: 409 insufficient stock).
 *
 * DEC-009 (inventory concurrency strategy) remains OPEN.
 * This exception reflects cart-time availability checking only — no stock reservation.
 * Authoritative stock enforcement occurs at checkout/order-creation time.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
