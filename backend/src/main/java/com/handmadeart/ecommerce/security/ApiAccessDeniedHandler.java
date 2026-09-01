package com.handmadeart.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a structured {@link ApiError} JSON response (403) when an authenticated
 * principal lacks the required role or permission for an endpoint.
 *
 * This is the companion to {@link AuthEntryPoint} (401 for unauthenticated requests).
 * Consistent with the approved error envelope (SDD §12.1–12.2).
 *
 * 401 vs 403 semantics (SDD §8.6, REST API Spec §2):
 *   401 UNAUTHORIZED — no valid authentication present (handled by AuthEntryPoint).
 *   403 FORBIDDEN    — authenticated but insufficient role/permission (handled here).
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "You do not have permission to access this resource",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
