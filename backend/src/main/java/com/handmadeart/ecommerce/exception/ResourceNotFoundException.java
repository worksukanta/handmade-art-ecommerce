package com.handmadeart.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource cannot be found or is not visible to the caller.
 *
 * Mapped to 404 Not Found by {@link GlobalExceptionHandler}.
 * The message is safe to return in the API response — it must not reveal
 * information that would assist enumeration attacks.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
