package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.exception.ApiError;
import com.handmadeart.ecommerce.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void oversizedMultipartUploadReturnsNormalizedPayloadTooLargeError() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/custom-requests/10/images");

        var response = new GlobalExceptionHandler().handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(10L * 1024 * 1024), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(413);
        assertThat(body.getError()).isEqualTo("UPLOAD_TOO_LARGE");
        assertThat(body.getMessage()).isEqualTo("Uploaded file exceeds the configured size limit");
        assertThat(body.getPath()).isEqualTo("/api/v1/custom-requests/10/images");
    }
    @Test
    void databaseConstraintDetailsAreNotExposed() {
        var response = new GlobalExceptionHandler().handleDataConflict(
                new org.springframework.dao.DataIntegrityViolationException("SQL insert password_hash internal path"),
                new MockHttpServletRequest("POST", "/api/v1/account/addresses"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Request conflicts with existing data or constraints");
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalDetails() {
        var response = new GlobalExceptionHandler().handleUnexpected(
                new RuntimeException("SQL password_hash internal path"),
                new MockHttpServletRequest("GET", "/api/v1/products"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

}
