package com.handmadeart.ecommerce.exception;

/**
 * Thrown when an attempt is made to create or rename a category to a name
 * that already exists in the system.
 *
 * Produces a 409 Conflict response via {@link GlobalExceptionHandler}.
 */
public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String message) {
        super(message);
    }
}
