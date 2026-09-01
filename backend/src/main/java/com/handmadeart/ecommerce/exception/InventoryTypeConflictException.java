package com.handmadeart.ecommerce.exception;

/**
 * Thrown when an inventory operation is attempted on a product type that does not
 * support inventory management (e.g., PORTFOLIO_ONLY products).
 *
 * Produces a 409 Conflict response via {@link GlobalExceptionHandler}.
 *
 * Using a specific exception type rather than the broad {@link IllegalStateException}
 * prevents unrelated JVM/framework state exceptions from being incorrectly mapped to
 * a 409 client response.
 */
public class InventoryTypeConflictException extends RuntimeException {

    public InventoryTypeConflictException(String message) {
        super(message);
    }
}
