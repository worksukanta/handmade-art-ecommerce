package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CheckoutService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.OrderService;
import com.handmadeart.ecommerce.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization and role-enforcement tests for Phase 3A.2.
 *
 * Tests the real security filter chain (SecurityConfig imported), JWT filter,
 * 401/403 handlers, and route-level role enforcement — without a database.
 *
 * Strategy for ADMIN authorization testing:
 *   /api/v1/admin/** is protected by route rule hasRole("ADMIN").
 *   Spring Security evaluates this BEFORE reaching any controller.
 *   CUSTOMER token → 403 (rejected by filter chain).
 *   ADMIN token    → 404 (authorized; no controller registered for this path yet).
 *   No auth        → 401 (rejected by filter chain before role check).
 *   This tests the security rules truthfully without inventing business endpoints.
 *
 * Covered:
 *   AZ-01  No token → protected resource → 401
 *   AZ-02  Valid CUSTOMER JWT → CUSTOMER resource (/me) → 200
 *   AZ-03  Valid CUSTOMER JWT → ADMIN resource → 403 with structured error
 *   AZ-04  Valid ADMIN JWT → ADMIN resource → passes security (404 = no handler, not 403)
 *   AZ-05  403 response uses the approved JSON error envelope
 *   AZ-06  401 response uses the approved JSON error envelope
 *   AZ-07  Authenticated identity derived from JWT, not client-supplied header
 *   AZ-08  Expired token → protected resource → 401
 */
@WebMvcTest
@Import({
        SecurityAuthorizationTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private CatalogueService catalogueService;

    @MockitoBean
    private AdminCatalogueService adminCatalogueService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentService paymentService;

    // -------------------------------------------------------------------------
    // Test configuration
    // -------------------------------------------------------------------------

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        public JwtService jwtService(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expMs) {
            return new JwtService(secret, expMs);
        }
    }

    // -------------------------------------------------------------------------
    // Token helpers
    // -------------------------------------------------------------------------

    private String customerToken(String email) {
        return jwtService.generateToken(email, "CUSTOMER");
    }

    private String adminToken(String email) {
        return jwtService.generateToken(email, "ADMIN");
    }

    private UserDetails customerDetails(String email) {
        return User.builder().username(email).password("{noop}x").roles("CUSTOMER").build();
    }

    private UserDetails adminDetails(String email) {
        return User.builder().username(email).password("{noop}x").roles("ADMIN").build();
    }

    // -------------------------------------------------------------------------
    // AZ-01: No token → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-01: Unauthenticated request to protected resource returns 401")
    void noToken_protectedResource_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // AZ-02: CUSTOMER JWT → CUSTOMER resource (/me) → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-02: Valid CUSTOMER JWT accepted for customer-authenticated endpoint (/me)")
    void customerToken_meEndpoint_returns200() throws Exception {
        String email = "alice@example.com";

        // AuthService and UserDetailsService are mocked — no DB needed
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails(email));
        when(authService.getCurrentUser(anyString()))
                .thenReturn(buildUserResponse(email, "CUSTOMER"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    // -------------------------------------------------------------------------
    // AZ-03: CUSTOMER JWT → ADMIN resource → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-03: CUSTOMER JWT denied access to ADMIN-only path returns 403")
    void customerToken_adminPath_returns403() throws Exception {
        String email = "alice@example.com";
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails(email));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + customerToken(email)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // AZ-04: ADMIN JWT → ADMIN resource → passes security (404 = no handler yet)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-04: ADMIN JWT passes security check on ADMIN-only path (no business handler yet = 404)")
    void adminToken_adminPath_passesSecurityReturns404() throws Exception {
        String email = "admin@example.com";
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(adminDetails(email));

        // 404 means Spring Security authorized the request — no controller registered yet.
        // This is the correct result: authorization passed, no handler found.
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken(email)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // AZ-05: 403 uses approved error envelope
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-05: 403 Forbidden response uses the approved JSON error envelope")
    void customerToken_adminPath_403HasStructuredErrorEnvelope() throws Exception {
        String email = "alice@example.com";
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails(email));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + customerToken(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/orders"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // -------------------------------------------------------------------------
    // AZ-06: 401 uses approved error envelope
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-06: 401 Unauthorized response uses the approved JSON error envelope")
    void noToken_401HasStructuredErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // -------------------------------------------------------------------------
    // AZ-07: Identity taken from JWT, not client-supplied header
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-07: Identity is derived from JWT sub claim, not any client-supplied header")
    void identity_derivedFromJwt_notClientHeader() throws Exception {
        String email = "alice@example.com";
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails(email));
        when(authService.getCurrentUser(anyString()))
                .thenReturn(buildUserResponse(email, "CUSTOMER"));

        // Supply a fraudulent X-User-Id header attempting to impersonate another user.
        // The JWT sub claim "alice@example.com" is what matters — the header is ignored.
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken(email))
                        .header("X-User-Id", "999"))   // ignored by the security layer
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    // -------------------------------------------------------------------------
    // AZ-08: Expired token → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AZ-08: Expired JWT returns 401 for any protected endpoint")
    void expiredToken_returns401() throws Exception {
        String expiredToken = jwtService.generateTokenWithExpiry(
                "alice@example.com", "CUSTOMER", -1000L);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helper — builds a minimal UserResponse for mocking
    // -------------------------------------------------------------------------

    private com.handmadeart.ecommerce.dto.auth.UserResponse buildUserResponse(
            String email, String role) {

        com.handmadeart.ecommerce.entity.AppUser user =
                new com.handmadeart.ecommerce.entity.AppUser();
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(com.handmadeart.ecommerce.entity.UserRole.valueOf(role));
        return com.handmadeart.ecommerce.dto.auth.UserResponse.from(user);
    }
}
