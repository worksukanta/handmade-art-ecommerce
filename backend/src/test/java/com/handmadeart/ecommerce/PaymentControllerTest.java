package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.PaymentController;
import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.OrderNotPayableException;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for POST/GET /api/v1/orders/{id}/payments.
 *
 * Covered:
 *   PAY-C-01  Unauthenticated POST /orders/{id}/payments → 401
 *   PAY-C-02  CUSTOMER POST /orders/{id}/payments valid → 201 + PaymentResponse
 *   PAY-C-03  CUSTOMER POST /orders/{id}/payments missing paymentMethod → 400
 *   PAY-C-04  CUSTOMER POST /orders/{id}/payments foreign orderId → 404
 *   PAY-C-05  CUSTOMER POST /orders/{id}/payments already confirmed order → 409 ORDER_NOT_PAYABLE
 *   PAY-C-06  ADMIN POST /orders/{id}/payments → 403
 *   PAY-C-07  CUSTOMER GET /orders/{id}/payments → 200 + list
 *   PAY-C-08  Unauthenticated GET /orders/{id}/payments → 401
 */
@WebMvcTest(PaymentController.class)
@Import({
        PaymentControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private PaymentService paymentService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private AuthService authService;
    @MockitoBean private CatalogueService catalogueService;
    @MockitoBean private AdminCatalogueService adminCatalogueService;
    @MockitoBean private CartService cartService;
    @MockitoBean private CheckoutService checkoutService;
    @MockitoBean private OrderService orderService;

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

    private PaymentResponse buildPaymentResponse() {
        PaymentResponse r = new PaymentResponse();
        return r;
    }

    // -------------------------------------------------------------------------
    // PAY-C-01: Unauthenticated POST /orders/{id}/payments → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-01: Unauthenticated POST /orders/{id}/payments returns 401")
    void unauthenticated_postPayment_returns401() throws Exception {
        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        mockMvc.perform(post("/api/v1/orders/10/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // PAY-C-02: CUSTOMER POST valid request → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-02: CUSTOMER POST /orders/{id}/payments with valid request returns 201")
    void customerToken_validPayment_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(paymentService.initiatePayment(any(), anyLong(), any()))
                .thenReturn(buildPaymentResponse());

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        mockMvc.perform(post("/api/v1/orders/10/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // PAY-C-03: Missing paymentMethod → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-03: POST /orders/{id}/payments with missing paymentMethod returns 400")
    void customerToken_missingPaymentMethod_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        // paymentMethod not set
        PaymentInitiationRequest req = new PaymentInitiationRequest();

        mockMvc.perform(post("/api/v1/orders/10/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // PAY-C-04: Foreign orderId → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-04: POST /orders/{id}/payments with foreign orderId returns 404")
    void customerToken_foreignOrderId_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(paymentService.initiatePayment(any(), anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        mockMvc.perform(post("/api/v1/orders/999/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PAY-C-05: Already confirmed order → 409 ORDER_NOT_PAYABLE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-05: POST /orders/{id}/payments on already-confirmed order returns 409 ORDER_NOT_PAYABLE")
    void customerToken_alreadyConfirmedOrder_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(paymentService.initiatePayment(any(), anyLong(), any()))
                .thenThrow(new OrderNotPayableException("Order 10 is not payable (status: CONFIRMED)"));

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        mockMvc.perform(post("/api/v1/orders/10/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_PAYABLE"));
    }

    // -------------------------------------------------------------------------
    // PAY-C-06: ADMIN POST → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-06: ADMIN JWT on POST /orders/{id}/payments returns 403")
    void adminToken_postPayment_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        mockMvc.perform(post("/api/v1/orders/10/payments")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // PAY-C-07: CUSTOMER GET /orders/{id}/payments → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-07: CUSTOMER GET /orders/{id}/payments returns 200 + payment list")
    void customerToken_getPayments_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(paymentService.getOrderPayments(any(), anyLong()))
                .thenReturn(List.of(buildPaymentResponse()));

        mockMvc.perform(get("/api/v1/orders/10/payments")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PAY-C-08: Unauthenticated GET /orders/{id}/payments → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PAY-C-08: Unauthenticated GET /orders/{id}/payments returns 401")
    void unauthenticated_getPayments_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/10/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
