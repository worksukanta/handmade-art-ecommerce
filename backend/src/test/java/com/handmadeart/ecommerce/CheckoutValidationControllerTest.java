package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.CheckoutController;
import com.handmadeart.ecommerce.dto.order.CheckoutValidationResponse;
import com.handmadeart.ecommerce.dto.order.CreateOrderRequest;
import com.handmadeart.ecommerce.entity.AppUser;
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
import com.handmadeart.ecommerce.service.CheckoutValidationService;
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
 * MockMvc controller tests for POST /api/v1/checkout/validate.
 *
 * Covered:
 *   CHK-V-01  Unauthenticated → 401
 *   CHK-V-02  CUSTOMER valid cart/address → 200 + CheckoutValidationResponse (valid=true)
 *   CHK-V-03  Missing addressId → 400
 *   CHK-V-04  Empty cart → 409 EMPTY_CART
 *   CHK-V-05  Foreign address → 404 NOT_FOUND
 *   CHK-V-06  Insufficient stock → 409 INSUFFICIENT_STOCK
 *   CHK-V-07  Non-purchasable product → 409 PRODUCT_NOT_PURCHASABLE
 *   CHK-V-08  ADMIN JWT → 403 (CUSTOMER only)
 */
@WebMvcTest(CheckoutController.class)
@Import({
        CheckoutValidationControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CheckoutValidationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private CheckoutService checkoutService;
    @MockitoBean private CheckoutValidationService checkoutValidationService;
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

    private AppUser buildCustomer() {
        AppUser user = new AppUser();
        user.setEmail("customer@example.com");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private CheckoutValidationResponse buildValidResponse() {
        CheckoutValidationResponse r = new CheckoutValidationResponse();
        r.setValid(true);
        r.setItems(List.of());
        r.setSubtotalAmount(BigDecimal.valueOf(50));
        r.setTotalAmount(BigDecimal.valueOf(50));
        return r;
    }

    // -------------------------------------------------------------------------
    // CHK-V-01: Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-01: Unauthenticated POST /checkout/validate returns 401")
    void unauthenticated_checkoutValidate_returns401() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // CHK-V-02: CUSTOMER valid cart/address → 200 + valid=true
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-02: CUSTOMER POST /checkout/validate valid request returns 200 + valid=true")
    void customerToken_validRequest_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(checkoutValidationService.validate(any(), any())).thenReturn(buildValidResponse());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.subtotalAmount").value(50));
    }

    // -------------------------------------------------------------------------
    // CHK-V-03: Missing addressId → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-03: POST /checkout/validate missing addressId returns 400")
    void customerToken_missingAddressId_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        CreateOrderRequest req = new CreateOrderRequest(); // no addressId

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // CHK-V-04: Empty cart → 409 EMPTY_CART
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-04: POST /checkout/validate empty cart returns 409 EMPTY_CART")
    void customerToken_emptyCart_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(checkoutValidationService.validate(any(), any()))
                .thenThrow(new EmptyCartException("Cart is empty"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMPTY_CART"));
    }

    // -------------------------------------------------------------------------
    // CHK-V-05: Foreign address → 404 NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-05: POST /checkout/validate foreign address returns 404")
    void customerToken_foreignAddress_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(checkoutValidationService.validate(any(), any()))
                .thenThrow(new ResourceNotFoundException("Address not found"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(999L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CHK-V-06: Insufficient stock → 409 INSUFFICIENT_STOCK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-06: POST /checkout/validate insufficient stock returns 409")
    void customerToken_insufficientStock_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(checkoutValidationService.validate(any(), any()))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    // -------------------------------------------------------------------------
    // CHK-V-07: Non-purchasable product → 409 PRODUCT_NOT_PURCHASABLE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-07: POST /checkout/validate non-purchasable product returns 409")
    void customerToken_nonPurchasableProduct_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(checkoutValidationService.validate(any(), any()))
                .thenThrow(new ProductNotPurchasableException("Product not eligible"));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_PURCHASABLE"));
    }

    // -------------------------------------------------------------------------
    // CHK-V-08: ADMIN JWT → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CHK-V-08: ADMIN POST /checkout/validate returns 403")
    void adminToken_checkoutValidate_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(1L);

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
