package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.OrderController;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.dto.order.OrderSummaryResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CheckoutService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for the customer Order read endpoints.
 *
 * Covered:
 *   ORD-C-01  Unauthenticated GET /orders → 401
 *   ORD-C-02  CUSTOMER GET /orders → 200 + paginated list
 *   ORD-C-03  CUSTOMER GET /orders/{id} → 200 + OrderResponse
 *   ORD-C-04  CUSTOMER GET /orders/{id} with foreign id → 404
 *   ORD-C-05  ADMIN GET /orders → 403 (CUSTOMER-only)
 *   ORD-C-06  Unauthenticated GET /orders/{id} → 401
 */
@WebMvcTest(OrderController.class)
@Import({
        OrderControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private OrderService orderService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private AuthService authService;
    @MockitoBean private CatalogueService catalogueService;
    @MockitoBean private AdminCatalogueService adminCatalogueService;
    @MockitoBean private CartService cartService;
    @MockitoBean private CheckoutService checkoutService;
    @MockitoBean private PaymentService paymentService;

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
    // Token / mock helpers
    // -------------------------------------------------------------------------

    private String customerToken() {
        return "Bearer " + jwtService.generateToken("customer@example.com", "CUSTOMER");
    }

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin@example.com", "ADMIN");
    }

    private UserDetails customerDetails() {
        return User.builder().username("customer@example.com")
                .password("{noop}x").roles("CUSTOMER").build();
    }

    private UserDetails adminDetails() {
        return User.builder().username("admin@example.com")
                .password("{noop}x").roles("ADMIN").build();
    }

    private AppUser buildCustomerEntity() {
        AppUser user = new AppUser();
        user.setEmail("customer@example.com");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private PageResponse<OrderSummaryResponse> buildEmptyPage() {
        PageResponse<OrderSummaryResponse> page = new PageResponse<>();
        return page;
    }

    // -------------------------------------------------------------------------
    // ORD-C-01: Unauthenticated GET /orders → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-01: Unauthenticated GET /orders returns 401")
    void unauthenticated_getOrders_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // ORD-C-02: CUSTOMER GET /orders → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-02: CUSTOMER GET /orders returns 200 + paginated order list")
    void customerToken_getOrders_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(orderService.getOrderHistory(any(), anyInt(), anyInt()))
                .thenReturn(buildEmptyPage());

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ORD-C-03: CUSTOMER GET /orders/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-03: CUSTOMER GET /orders/{id} returns 200 + OrderResponse")
    void customerToken_getOrderById_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());

        OrderResponse orderResponse = new OrderResponse();
        when(orderService.getOrderDetail(any(), anyLong())).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/10")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ORD-C-04: Foreign orderId → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-04: CUSTOMER GET /orders/{id} with foreign id returns 404")
    void customerToken_foreignOrderId_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(orderService.getOrderDetail(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/999")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ORD-C-05: ADMIN GET /orders → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-05: ADMIN JWT on GET /orders returns 403 (CUSTOMER role only)")
    void adminToken_getOrders_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // ORD-C-06: Unauthenticated GET /orders/{id} → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ORD-C-06: Unauthenticated GET /orders/{id} returns 401")
    void unauthenticated_getOrderById_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
