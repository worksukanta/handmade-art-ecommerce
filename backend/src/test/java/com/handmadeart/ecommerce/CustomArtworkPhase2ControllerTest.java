package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AdminCustomArtworkController;
import com.handmadeart.ecommerce.controller.CustomArtworkController;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentStatusUpdateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.entity.ShipmentStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AdminProductionService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CheckoutService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.CustomAdvancePaymentService;
import com.handmadeart.ecommerce.service.CustomArtworkRequestService;
import com.handmadeart.ecommerce.service.OrderService;
import com.handmadeart.ecommerce.service.PaymentService;
import com.handmadeart.ecommerce.service.QuotationService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for Phase 3E.2 custom artwork endpoints.
 *
 * Covered:
 *   CAR2-C-01  Unauthenticated POST /quotations/{id}/approve → 401
 *   CAR2-C-02  CUSTOMER POST /quotations/{id}/approve → 200
 *   CAR2-C-03  CUSTOMER POST /quotations/{id}/approve foreign → 404
 *   CAR2-C-04  CUSTOMER POST /quotations/{id}/approve invalid state → 409
 *   CAR2-C-05  CUSTOMER POST /quotations/{id}/reject → 200
 *   CAR2-C-06  ADMIN POST /quotations/{id}/approve → 403 (CUSTOMER-only)
 *   CAR2-C-07  CUSTOMER POST /custom-requests/{id}/payments → 201
 *   CAR2-C-08  CUSTOMER POST /custom-requests/{id}/payments invalid state → 409
 *   CAR2-C-09  CUSTOMER GET /custom-requests/{id}/payments → 200
 *   CAR2-C-10  CUSTOMER GET /custom-requests/{id}/shipment → 200
 *   CAR2-C-11  CUSTOMER GET /custom-requests/{id}/shipment no shipment → 404
 *   CAR2-C-12  ADMIN PATCH /admin/custom-requests/{id}/status → 200
 *   CAR2-C-13  ADMIN PATCH /admin/custom-requests/{id}/status invalid transition → 409
 *   CAR2-C-14  CUSTOMER denied admin production status endpoint → 403
 *   CAR2-C-15  ADMIN POST /admin/shipments → 201
 *   CAR2-C-16  ADMIN PATCH /admin/shipments/{id}/status → 200
 *   CAR2-C-17  ADMIN GET /admin/shipments/{id} → 200
 *   CAR2-C-18  Unauthenticated GET /custom-requests/{id}/payments → 401
 */
@WebMvcTest({CustomArtworkController.class, AdminCustomArtworkController.class})
@Import({
        CustomArtworkPhase2ControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CustomArtworkPhase2ControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private CustomArtworkRequestService customArtworkRequestService;
    @MockitoBean private QuotationService quotationService;
    @MockitoBean private CustomAdvancePaymentService advancePaymentService;
    @MockitoBean private AdminProductionService adminProductionService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;

    // Required by the full security context
    @MockitoBean private AuthService authService;
    @MockitoBean private CatalogueService catalogueService;
    @MockitoBean private AdminCatalogueService adminCatalogueService;
    @MockitoBean private CartService cartService;
    @MockitoBean private CheckoutService checkoutService;
    @MockitoBean private OrderService orderService;
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

    private AppUser buildAdminEntity() {
        AppUser user = new AppUser();
        user.setEmail("admin@example.com");
        user.setFullName("Test Admin");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    private QuotationResponse buildApprovedQuotation() {
        QuotationResponse r = new QuotationResponse();
        return r;
    }

    private PaymentResponse buildAdvancePaymentResponse() {
        PaymentResponse r = new PaymentResponse();
        return r;
    }

    private ShipmentResponse buildShipmentResponse() {
        ShipmentResponse r = new ShipmentResponse();
        return r;
    }

    // -------------------------------------------------------------------------
    // CAR2-C-01: Unauthenticated POST /quotations/{id}/approve → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-01: Unauthenticated POST /quotations/{id}/approve returns 401")
    void unauthenticated_approveQuotation_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/quotations/100/approve"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-02: CUSTOMER POST /quotations/{id}/approve → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-02: CUSTOMER POST /quotations/{id}/approve returns 200")
    void customerToken_approveQuotation_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.approveQuotation(any(), anyLong()))
                .thenReturn(buildApprovedQuotation());

        mockMvc.perform(post("/api/v1/quotations/100/approve")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-03: CUSTOMER POST /quotations/{id}/approve foreign → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-03: CUSTOMER POST /quotations/{id}/approve for foreign quotation returns 404")
    void customerToken_approveForeignQuotation_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.approveQuotation(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Quotation not found"));

        mockMvc.perform(post("/api/v1/quotations/999/approve")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CAR2-C-04: CUSTOMER POST /quotations/{id}/approve invalid state → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-04: CUSTOMER POST /quotations/{id}/approve with invalid state returns 409")
    void customerToken_approveQuotation_invalidState_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.approveQuotation(any(), anyLong()))
                .thenThrow(new InvalidWorkflowTransitionException("Cannot approve: already decided"));

        mockMvc.perform(post("/api/v1/quotations/100/approve")
                        .header("Authorization", customerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // -------------------------------------------------------------------------
    // CAR2-C-05: CUSTOMER POST /quotations/{id}/reject → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-05: CUSTOMER POST /quotations/{id}/reject returns 200")
    void customerToken_rejectQuotation_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.rejectQuotation(any(), anyLong()))
                .thenReturn(buildApprovedQuotation());

        mockMvc.perform(post("/api/v1/quotations/100/reject")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-06: ADMIN POST /quotations/{id}/approve → 403 (CUSTOMER-only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-06: ADMIN POST /quotations/{id}/approve returns 403 (CUSTOMER-only endpoint)")
    void adminToken_approveQuotation_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        mockMvc.perform(post("/api/v1/quotations/100/approve")
                        .header("Authorization", adminToken()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-07: CUSTOMER POST /custom-requests/{id}/payments → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-07: CUSTOMER POST /custom-requests/{id}/payments returns 201")
    void customerToken_initiateAdvancePayment_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(advancePaymentService.initiateAdvancePayment(any(), anyLong(), any()))
                .thenReturn(buildAdvancePaymentResponse());

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        mockMvc.perform(post("/api/v1/custom-requests/10/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-08: CUSTOMER POST /custom-requests/{id}/payments invalid state → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-08: CUSTOMER POST advance payment with invalid workflow state returns 409")
    void customerToken_initiateAdvancePayment_invalidState_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(advancePaymentService.initiateAdvancePayment(any(), anyLong(), any()))
                .thenThrow(new InvalidWorkflowTransitionException("Advance payment not allowed"));

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        mockMvc.perform(post("/api/v1/custom-requests/10/payments")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // -------------------------------------------------------------------------
    // CAR2-C-09: CUSTOMER GET /custom-requests/{id}/payments → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-09: CUSTOMER GET /custom-requests/{id}/payments returns 200")
    void customerToken_getCustomRequestPayments_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(advancePaymentService.getCustomRequestPayments(any(), anyLong()))
                .thenReturn(List.of(buildAdvancePaymentResponse()));

        mockMvc.perform(get("/api/v1/custom-requests/10/payments")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-10: CUSTOMER GET /custom-requests/{id}/shipment → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-10: CUSTOMER GET /custom-requests/{id}/shipment returns 200")
    void customerToken_getCustomRequestShipment_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(advancePaymentService.getCustomRequestShipment(any(), anyLong()))
                .thenReturn(buildShipmentResponse());

        mockMvc.perform(get("/api/v1/custom-requests/10/shipment")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-11: CUSTOMER GET /custom-requests/{id}/shipment no shipment → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-11: CUSTOMER GET /custom-requests/{id}/shipment when none exists returns 404")
    void customerToken_getCustomRequestShipment_noShipment_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(advancePaymentService.getCustomRequestShipment(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Shipment not found"));

        mockMvc.perform(get("/api/v1/custom-requests/10/shipment")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-12: ADMIN PATCH /admin/custom-requests/{id}/status → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-12: ADMIN PATCH /admin/custom-requests/{id}/status returns 200")
    void adminToken_updateProductionStatus_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.updateCustomRequestStatus(anyLong(), any()))
                .thenReturn(new com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse());

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-13: ADMIN PATCH /admin/custom-requests/{id}/status invalid transition → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-13: ADMIN PATCH production status with invalid transition returns 409")
    void adminToken_updateProductionStatus_invalidTransition_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.updateCustomRequestStatus(anyLong(), any()))
                .thenThrow(new InvalidWorkflowTransitionException("Invalid transition"));

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELIVERED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // -------------------------------------------------------------------------
    // CAR2-C-14: CUSTOMER denied admin production status endpoint → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-14: CUSTOMER PATCH admin production status endpoint returns 403")
    void customerToken_updateProductionStatus_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/status")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-15: ADMIN POST /admin/shipments → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-15: ADMIN POST /admin/shipments returns 201")
    void adminToken_createShipment_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.createShipment(any()))
                .thenReturn(buildShipmentResponse());

        com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest createReq =
                new com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest();
        createReq.setCustomOrderRequestId(10L);

        mockMvc.perform(post("/api/v1/admin/shipments")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-16: ADMIN PATCH /admin/shipments/{id}/status → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-16: ADMIN PATCH /admin/shipments/{id}/status returns 200")
    void adminToken_updateShipmentStatus_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.updateShipmentStatus(anyLong(), any()))
                .thenReturn(buildShipmentResponse());

        ShipmentStatusUpdateRequest statusReq = new ShipmentStatusUpdateRequest();
        statusReq.setStatus(ShipmentStatus.SHIPPED);

        mockMvc.perform(patch("/api/v1/admin/shipments/300/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-17: ADMIN GET /admin/shipments/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-17: ADMIN GET /admin/shipments/{id} returns 200")
    void adminToken_getShipment_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.adminGetShipment(anyLong()))
                .thenReturn(buildShipmentResponse());

        mockMvc.perform(get("/api/v1/admin/shipments/300")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR2-C-18: Unauthenticated GET /custom-requests/{id}/payments → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR2-C-18: Unauthenticated GET /custom-requests/{id}/payments returns 401")
    void unauthenticated_getCustomRequestPayments_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/custom-requests/10/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN can read quotation by custom request ID")
    void adminToken_getQuotationByRequestId_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(quotationService.adminGetQuotationByRequestId(10L)).thenReturn(buildApprovedQuotation());

        mockMvc.perform(get("/api/v1/admin/custom-requests/10/quotation")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN quotation by request returns normalized not found")
    void adminToken_getQuotationByRequestId_missing_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(quotationService.adminGetQuotationByRequestId(10L))
                .thenThrow(new ResourceNotFoundException("Quotation not found for custom request 10"));

        mockMvc.perform(get("/api/v1/admin/custom-requests/10/quotation")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("ADMIN can read payments by custom request ID")
    void adminToken_getPaymentsByRequestId_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(advancePaymentService.adminGetCustomRequestPayments(10L))
                .thenReturn(List.of(buildAdvancePaymentResponse()));

        mockMvc.perform(get("/api/v1/admin/custom-requests/10/payments")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("ADMIN payment history is empty when the request has no payments")
    void adminToken_getPaymentsByRequestId_empty_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(advancePaymentService.adminGetCustomRequestPayments(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/custom-requests/10/payments")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("ADMIN can read shipment by custom request ID")
    void adminToken_getShipmentByRequestId_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminProductionService.adminGetShipmentByRequestId(10L)).thenReturn(buildShipmentResponse());

        mockMvc.perform(get("/api/v1/admin/custom-requests/10/shipment")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CUSTOMER and anonymous callers cannot read ADMIN request-scoped resources")
    void nonAdmin_getRequestScopedAdminResources_areRejected() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        for (String resource : List.of("quotation", "payments", "shipment")) {
            String path = "/api/v1/admin/custom-requests/10/" + resource;
            mockMvc.perform(get(path).header("Authorization", customerToken()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());
        }
    }
}
