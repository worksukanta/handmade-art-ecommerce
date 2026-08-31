package com.handmadeart.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consistent REST error envelope returned for all API error responses.
 *
 * Shape defined in System Design Document §12.2:
 * {
 *   "timestamp": "<ISO-8601>",
 *   "status":    <HTTP status code>,
 *   "error":     "<short category>",
 *   "message":   "<safe message>",
 *   "path":      "<request path>",
 *   "details":   [ <optional field-level messages> ]
 * }
 *
 * Stack traces and internal details are never included.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private OffsetDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> details;

    public ApiError() {
    }

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiError(int status, String error, String message, String path, List<String> details) {
        this(status, error, message, path);
        this.details = details;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<String> getDetails() {
        return details;
    }
}
