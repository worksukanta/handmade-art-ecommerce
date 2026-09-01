package com.handmadeart.ecommerce.exception;

/**
 * Thrown when an Admin attempts to create a quotation for a custom artwork request
 * that already has one.
 *
 * One-quotation-per-request constraint: enforced by the UNIQUE index on
 * {@code quotation.custom_order_request_id} (Database Design &amp; ERD §13.2,
 * DEC-004 DEFERRED — re-quotation not implemented).
 *
 * Mapped to 409 DUPLICATE_QUOTATION by {@link GlobalExceptionHandler}.
 */
public class DuplicateQuotationException extends RuntimeException {

    public DuplicateQuotationException(String message) {
        super(message);
    }
}
