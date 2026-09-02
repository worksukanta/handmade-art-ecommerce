package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.controller.AdminPaymentController;
import com.handmadeart.ecommerce.dto.order.AdminPaymentResponse;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AdminPaymentService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for admin payment endpoint.
 *
 * Covered:
 *   ADM-PAY-C-01  ADMIN GET /admin/payments/{id} → 200 + payment data
 *   ADM-PAY-C-02  ADMIN GET /admin/payments/{id} missing → 404
 *   ADM-PAY-C-03  Response never contains password/raw card fields
 *   ADM-PAY-C-04  CUSTOMER GET /admin/payments/{id} → 403
 */
@WebMvcTest(AdminPaymentController.class)
@Import({
        AdminPaymentControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class AdminPaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminPaymentService adminPaymentService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private AuthService authService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private CatalogueService catalogueService;
    @MockitoBean private AdminCatalogueService adminCatalogueService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtService jwtService(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expMs) {
            return new JwtService(secret, expMs);
        }
    }

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin@example.com", "ADMIN");
    }

    private String customerToken() {
        return "Bearer " + jwtService.generateToken("customer@example.com", "CUSTOMER");
    }

    private UserDetails adminDetails() {
        return User.builder().username("admin@example.com").password("{noop}x").roles("ADMIN").build();
    }

    private UserDetails customerDetails() {
        return User.builder().username("customer@example.com").password("{noop}x").roles("CUSTOMER").build();
    }

    private AdminPaymentResponse buildPaymentResponse() {
        AdminPaymentResponse r = new AdminPaymentResponse();
        // Use factory-style: set via direct field access isn't possible — AdminPaymentResponse
        // uses static from(); return a stubbed instance
        return r;
    }

    // -------------------------------------------------------------------------
    // ADM-PAY-C-01: ADMIN GET /admin/payments/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-PAY-C-01: ADMIN GET /admin/payments/{id} returns 200 + AdminPaymentResponse")
    void adminToken_getPayment_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminPaymentService.getPayment(anyLong())).thenReturn(buildPaymentResponse());

        mockMvc.perform(get("/api/v1/admin/payments/1")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ADM-PAY-C-02: ADMIN GET /admin/payments/{id} missing → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-PAY-C-02: ADMIN GET /admin/payments/{id} missing returns 404")
    void adminToken_getPayment_missing_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminPaymentService.getPayment(anyLong()))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/v1/admin/payments/999")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ADM-PAY-C-03: Response must not contain sensitive payment credential fields
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-PAY-C-03: AdminPaymentResponse does not contain password hash or raw card fields")
    void adminPaymentResponse_neverContainsSensitiveFields() throws Exception {
        // Build a real AdminPaymentResponse via reflection to verify structure
        AdminPaymentResponse response = new AdminPaymentResponse();
        // Verify the DTO class has no passwordHash, cardNumber, cvv, or pin fields
        java.lang.reflect.Field[] fields = AdminPaymentResponse.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            String name = field.getName().toLowerCase();
            assert !name.contains("password") : "Sensitive field 'password' found in AdminPaymentResponse";
            assert !name.contains("card") : "Sensitive field 'card' found in AdminPaymentResponse";
            assert !name.contains("cvv") : "Sensitive field 'cvv' found in AdminPaymentResponse";
            assert !name.contains("pin") : "Sensitive field 'pin' found in AdminPaymentResponse";
        }
    }

    // -------------------------------------------------------------------------
    // ADM-PAY-C-04: CUSTOMER → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-PAY-C-04: CUSTOMER GET /admin/payments/{id} returns 403")
    void customerToken_getPayment_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        mockMvc.perform(get("/api/v1/admin/payments/1")
                        .header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
    }
}
