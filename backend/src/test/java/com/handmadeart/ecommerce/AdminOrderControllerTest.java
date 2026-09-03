package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AdminOrderController;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderResponse;
import com.handmadeart.ecommerce.dto.order.AdminOrderStatusRequest;
import com.handmadeart.ecommerce.dto.order.AdminOrderSummaryResponse;
import com.handmadeart.ecommerce.dto.order.AdminPaymentResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AdminOrderService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for admin order endpoints.
 *
 * Covered:
 *   ADM-ORD-C-01  Unauthenticated GET /admin/orders → 401
 *   ADM-ORD-C-02  CUSTOMER GET /admin/orders → 403
 *   ADM-ORD-C-03  ADMIN GET /admin/orders → 200 + page
 *   ADM-ORD-C-04  ADMIN GET /admin/orders/{id} → 200
 *   ADM-ORD-C-05  ADMIN GET /admin/orders/{id} missing → 404
 *   ADM-ORD-C-06  ADMIN PATCH /admin/orders/{id}/status valid transition → 200
 *   ADM-ORD-C-07  ADMIN PATCH /admin/orders/{id}/status invalid transition → 409
 *   ADM-ORD-C-08  CUSTOMER PATCH /admin/orders/{id}/status → 403
 */
@WebMvcTest(AdminOrderController.class)
@Import({
        AdminOrderControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class AdminOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminOrderService adminOrderService;
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
    // ADM-ORD-C-01: Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-01: Unauthenticated GET /admin/orders returns 401")
    void unauthenticated_listOrders_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-02: CUSTOMER → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-02: CUSTOMER GET /admin/orders returns 403")
    void customerToken_listOrders_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-03: ADMIN GET /admin/orders → 200 + page
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-03: ADMIN GET /admin/orders returns 200 + page")
    void adminToken_listOrders_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.listAllOrders(anyInt(), anyInt()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(new AdminOrderSummaryResponse()))));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-04: ADMIN GET /admin/orders/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-04: ADMIN GET /admin/orders/{id} returns 200")
    void adminToken_getOrder_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.getOrderDetail(anyLong())).thenReturn(new AdminOrderResponse());

        mockMvc.perform(get("/api/v1/admin/orders/1")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-05: ADMIN GET /admin/orders/{id} missing → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-05: ADMIN GET /admin/orders/{id} missing order returns 404")
    void adminToken_getOrder_missing_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.getOrderDetail(anyLong()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/admin/orders/999")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-06: ADMIN PATCH status valid transition → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-06: ADMIN PATCH /admin/orders/{id}/status valid transition returns 200")
    void adminToken_patchStatus_validTransition_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.updateOrderStatus(anyLong(), any(OrderStatus.class)))
                .thenReturn(new AdminOrderResponse());

        AdminOrderStatusRequest req = new AdminOrderStatusRequest();
        req.setStatus(OrderStatus.PROCESSING);

        mockMvc.perform(patch("/api/v1/admin/orders/1/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-07: ADMIN PATCH status invalid transition → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-07: ADMIN PATCH /admin/orders/{id}/status invalid transition returns 409")
    void adminToken_patchStatus_invalidTransition_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.updateOrderStatus(anyLong(), any(OrderStatus.class)))
                .thenThrow(new InvalidWorkflowTransitionException("Invalid transition"));

        AdminOrderStatusRequest req = new AdminOrderStatusRequest();
        req.setStatus(OrderStatus.DELIVERED);

        mockMvc.perform(patch("/api/v1/admin/orders/1/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // -------------------------------------------------------------------------
    // ADM-ORD-C-08: CUSTOMER PATCH status → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ADM-ORD-C-08: CUSTOMER PATCH /admin/orders/{id}/status returns 403")
    void customerToken_patchStatus_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        AdminOrderStatusRequest req = new AdminOrderStatusRequest();
        req.setStatus(OrderStatus.PROCESSING);

        mockMvc.perform(patch("/api/v1/admin/orders/1/status")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminToken_getOrderPayments_returns200Collection() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.getOrderPayments(1L)).thenReturn(List.of(new AdminPaymentResponse()));
        mockMvc.perform(get("/api/v1/admin/orders/1/payments").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminToken_getOrderShipment_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminOrderService.getOrderShipment(1L)).thenReturn(new ShipmentResponse());
        mockMvc.perform(get("/api/v1/admin/orders/1/shipment").header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void customerToken_cannotReadAdminOrderChildren() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        mockMvc.perform(get("/api/v1/admin/orders/1/payments").header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/orders/1/shipment").header("Authorization", customerToken()))
                .andExpect(status().isForbidden());
    }
}
