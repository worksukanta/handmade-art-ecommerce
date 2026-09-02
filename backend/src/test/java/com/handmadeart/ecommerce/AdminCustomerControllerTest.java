package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.controller.AdminCustomerController;
import com.handmadeart.ecommerce.dto.admin.AdminCustomerResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AdminCustomerService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for admin customer endpoints.
 *
 * Covered:
 *   ADM-CUST-C-01  Unauthenticated GET /admin/customers → 401
 *   ADM-CUST-C-02  CUSTOMER GET /admin/customers → 403
 *   ADM-CUST-C-03  ADMIN GET /admin/customers → 200 + page (no password fields)
 *   ADM-CUST-C-04  ADMIN GET /admin/customers/{id} → 200
 *   ADM-CUST-C-05  ADMIN GET /admin/customers/{id} missing → 404
 *   ADM-CUST-C-06  CUSTOMER GET /admin/customers/{id} → 403
 */
@WebMvcTest(AdminCustomerController.class)
@Import({
        AdminCustomerControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class AdminCustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminCustomerService adminCustomerService;
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

    // -------------------------------------------------------------------------
    // ADM-CUST-C-01: Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-01: Unauthenticated GET /admin/customers returns 401")
    void unauthenticated_listCustomers_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ADM-CUST-C-02: CUSTOMER → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-02: CUSTOMER GET /admin/customers returns 403")
    void customerToken_listCustomers_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // ADM-CUST-C-03: ADMIN GET /admin/customers → 200 + page
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-03: ADMIN GET /admin/customers returns 200 + page (no password fields)")
    void adminToken_listCustomers_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCustomerService.listCustomers(anyInt(), anyInt()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(new AdminCustomerResponse()))));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // No passwordHash field in response
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // ADM-CUST-C-04: ADMIN GET /admin/customers/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-04: ADMIN GET /admin/customers/{id} returns 200")
    void adminToken_getCustomer_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCustomerService.getCustomer(anyLong())).thenReturn(new AdminCustomerResponse());

        mockMvc.perform(get("/api/v1/admin/customers/1")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // ADM-CUST-C-05: ADMIN GET /admin/customers/{id} missing → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-05: ADMIN GET /admin/customers/{id} missing returns 404")
    void adminToken_getCustomer_missing_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCustomerService.getCustomer(anyLong()))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/api/v1/admin/customers/999")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ADM-CUST-C-06: CUSTOMER GET /admin/customers/{id} → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-CUST-C-06: CUSTOMER GET /admin/customers/{id} returns 403")
    void customerToken_getCustomer_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        mockMvc.perform(get("/api/v1/admin/customers/1")
                        .header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
    }
}
