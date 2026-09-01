package com.handmadeart.ecommerce.exception;

/**
 * Thrown when a requested workflow transition is not valid for the current state
 * of a {@link com.handmadeart.ecommerce.entity.CustomOrderRequest} or
 * {@link com.handmadeart.ecommerce.entity.Quotation}.
 *
 * Mapped to 409 INVALID_TRANSITION by {@link GlobalExceptionHandler}.
 *
 * Approved state transitions are enforced exclusively by the service layer
 * (Database Design &amp; ERD §12.3, CustomOrderRequestStatus Javadoc).
 */
public class InvalidWorkflowTransitionException extends RuntimeException {

    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}
