package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.CheckoutController;
import com.handmadeart.ecommerce.dto.order.CreateOrderRequest;
import com.handmadeart.ecommerce.dto.order.OrderItemResponse;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.EmptyCartException;
import com.handmadeart.ecommerce.exception.InsufficientStockException;
import com.handmadeart.ecommerce.exception.ProductNotPurchasableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CheckoutService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for POST /api/v1/orders (checkout/order creation).
 *
 * Uses the real SecurityConfig + JwtAuthenticationFilter to verify
 * authentication and authorization rules. CheckoutService is mocked.
 *
 * Covered:
 *   CHK-C-01  Unauthenticated POST /orders → 401
 *   CHK-C-02  CUSTOMER JWT + valid request → 201 + OrderResponse
 *   CHK-C-03  CUSTOMER JWT + missing addressId → 400
 *   CHK-C-04  CUSTOMER JWT + empty cart → 409 EMPTY_CART
 *   CHK-C-05  CUSTOMER JWT + foreign address → 404 NOT_FOUND
 *   CHK-C-06  CUSTOMER JWT + insufficient stock → 409 INSUFFICIENT_STOCK
 *   CHK-C-07  CUSTOMER JWT + non-purchasable product → 409 PRODUCT_NOT_PURCHASABLE
 *   CHK-C-08  ADMIN JWT → 403 (order endpoint is CUSTOMER-only)
 */
@WebMvcTest(CheckoutController.class)
@Import({
        CheckoutControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CheckoutControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private CheckoutService checkoutService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private AuthService authService;
    @MockitoBean private CatalogueService catalogueService;
    @MockitoBean private AdminCatalogueService adminCatalogueService;
    @MockitoBean private CartService cartService;

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

    private OrderResponse buildOrderResponse() {
        // Use a minimal valid OrderResponse
        OrderResponse r = new OrderResponse();
        return r;
    }

    // Reflection-free minimal OrderResponse for JSON assertions
    private OrderResponse buildFullOrderResponse() {
        // Can't easily construct via reflection-free; service mock returns it.
        return new OrderResponse();
    }

    // -------------------------------------------------------------------------
    // CHK-C-01: Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-01: Unauthenticated POST /orders returns 401")
    void unauthenticated_postOrders_returns401() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // CHK-C-02: CUSTOMER JWT + valid request → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-02: CUSTOMER POST /orders with valid request returns 201 + OrderResponse")
    void customerToken_validRequest_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(checkoutService.createOrder(any(), any())).thenReturn(buildOrderResponse());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // CHK-C-03: Missing addressId → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-03: POST /orders with missing addressId returns 400")
    void customerToken_missingAddressId_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());

        // addressId not set
        CreateOrderRequest req = new CreateOrderRequest();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // CHK-C-04: Empty cart → 409 EMPTY_CART
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-04: POST /orders with empty cart returns 409 EMPTY_CART")
    void customerToken_emptyCart_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(checkoutService.createOrder(any(), any()))
                .thenThrow(new EmptyCartException("Cart is empty"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("EMPTY_CART"));
    }

    // -------------------------------------------------------------------------
    // CHK-C-05: Foreign/missing address → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-05: POST /orders with foreign/missing address returns 404")
    void customerToken_foreignAddress_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(checkoutService.createOrder(any(), any()))
                .thenThrow(new ResourceNotFoundException("Address not found"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(999L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CHK-C-06: Insufficient stock → 409 INSUFFICIENT_STOCK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-06: POST /orders with insufficient stock returns 409 INSUFFICIENT_STOCK")
    void customerToken_insufficientStock_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(checkoutService.createOrder(any(), any()))
                .thenThrow(new InsufficientStockException("Insufficient stock for 'Widget': requested 5, available 2"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    // -------------------------------------------------------------------------
    // CHK-C-07: Non-purchasable product → 409 PRODUCT_NOT_PURCHASABLE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-07: POST /orders with non-purchasable product returns 409 PRODUCT_NOT_PURCHASABLE")
    void customerToken_nonPurchasableProduct_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(checkoutService.createOrder(any(), any()))
                .thenThrow(new ProductNotPurchasableException("Product is no longer available"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_PURCHASABLE"));
    }

    // -------------------------------------------------------------------------
    // CHK-C-08: ADMIN JWT → 403 (CUSTOMER-only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-C-08: ADMIN JWT on POST /orders returns 403 (CUSTOMER role only)")
    void adminToken_postOrders_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(adminDetails());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
