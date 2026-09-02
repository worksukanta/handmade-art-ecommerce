package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AdminCustomArtworkController;
import com.handmadeart.ecommerce.controller.CustomArtworkController;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomRequestReviewRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CheckoutService;
import com.handmadeart.ecommerce.service.CurrentUserService;
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
import java.time.OffsetDateTime;
import java.util.List;

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
 * MockMvc controller tests for Custom Artwork endpoints.
 *
 * Covered:
 *   CAR-C-01  Unauthenticated POST /custom-requests → 401
 *   CAR-C-02  CUSTOMER POST /custom-requests valid → 201 + response
 *   CAR-C-03  CUSTOMER POST /custom-requests missing required fields → 400
 *   CAR-C-04  CUSTOMER GET /custom-requests → 200 + page
 *   CAR-C-05  CUSTOMER GET /custom-requests/{id} owned → 200
 *   CAR-C-06  CUSTOMER GET /custom-requests/{id} foreign → 404
 *   CAR-C-07  ADMIN POST /custom-requests → 403 (CUSTOMER-only endpoint)
 *   CAR-C-08  CUSTOMER GET /custom-requests/{id}/quotation owned → 200
 *   CAR-C-09  CUSTOMER GET /custom-requests/{id}/quotation no quotation → 404
 *   CAR-C-10  CUSTOMER denied ADMIN review endpoint → 403
 *   CAR-C-11  ADMIN PATCH /admin/custom-requests/{id}/review → 200
 *   CAR-C-12  ADMIN PATCH /admin/custom-requests/{id}/review invalid transition → 409
 *   CAR-C-13  ADMIN POST /admin/custom-requests/{id}/quotation → 201
 *   CAR-C-14  ADMIN GET /admin/quotations/{id} → 200
 *   CAR-C-15  Unauthenticated GET /custom-requests → 401
 */
@WebMvcTest({CustomArtworkController.class, AdminCustomArtworkController.class})
@Import({
        CustomArtworkControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CustomArtworkControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private CustomArtworkRequestService customArtworkRequestService;
    @MockitoBean private QuotationService quotationService;
    @MockitoBean private com.handmadeart.ecommerce.service.CustomAdvancePaymentService advancePaymentService;
    @MockitoBean private com.handmadeart.ecommerce.service.AdminProductionService adminProductionService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;

    // Required by the full security context and other controllers loaded by WebMvcTest
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

    private CustomArtworkRequestResponse buildRequestResponse() {
        CustomArtworkRequestResponse r = new CustomArtworkRequestResponse();
        return r;
    }

    private QuotationResponse buildQuotationResponse() {
        QuotationResponse r = new QuotationResponse();
        return r;
    }

    // -------------------------------------------------------------------------
    // CAR-C-01: Unauthenticated POST /custom-requests → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-01: Unauthenticated POST /custom-requests returns 401")
    void unauthenticated_postCustomRequest_returns401() throws Exception {
        CustomArtworkRequestCreateRequest req = validCreateRequest();

        mockMvc.perform(post("/api/v1/custom-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // CAR-C-02: CUSTOMER POST valid → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-02: CUSTOMER POST /custom-requests with valid data returns 201")
    void customerToken_validCreate_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(customArtworkRequestService.createCustomRequest(any(), any()))
                .thenReturn(buildRequestResponse());

        mockMvc.perform(post("/api/v1/custom-requests")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // CAR-C-03: CUSTOMER POST missing required fields → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-03: CUSTOMER POST /custom-requests with missing description returns 400")
    void customerToken_missingDescription_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        CustomArtworkRequestCreateRequest req = new CustomArtworkRequestCreateRequest();
        req.setProductType("Oil Painting");
        // description is missing

        mockMvc.perform(post("/api/v1/custom-requests")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // -------------------------------------------------------------------------
    // CAR-C-04: CUSTOMER GET /custom-requests → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-04: CUSTOMER GET /custom-requests returns 200 + page")
    void customerToken_listRequests_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());

        PageResponse<CustomArtworkRequestSummary> emptyPage = new PageResponse<>();
        when(customArtworkRequestService.listCustomRequests(any(), any(Integer.class),
                any(Integer.class), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/custom-requests")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR-C-05: CUSTOMER GET /custom-requests/{id} owned → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-05: CUSTOMER GET /custom-requests/{id} for owned request returns 200")
    void customerToken_getOwnedRequest_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(customArtworkRequestService.getCustomRequest(any(), anyLong()))
                .thenReturn(buildRequestResponse());

        mockMvc.perform(get("/api/v1/custom-requests/10")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR-C-06: CUSTOMER GET /custom-requests/{id} foreign → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-06: CUSTOMER GET /custom-requests/{id} for foreign request returns 404")
    void customerToken_getForeignRequest_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(customArtworkRequestService.getCustomRequest(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Custom request not found"));

        mockMvc.perform(get("/api/v1/custom-requests/999")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CAR-C-07: ADMIN POST /custom-requests → 403 (CUSTOMER-only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-07: ADMIN POST /custom-requests returns 403 (CUSTOMER-only endpoint)")
    void adminToken_postCustomRequest_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        mockMvc.perform(post("/api/v1/custom-requests")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CAR-C-08: CUSTOMER GET /custom-requests/{id}/quotation owned → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-08: CUSTOMER GET /custom-requests/{id}/quotation returns 200")
    void customerToken_getOwnedQuotation_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.customerGetQuotation(any(), anyLong()))
                .thenReturn(buildQuotationResponse());

        mockMvc.perform(get("/api/v1/custom-requests/10/quotation")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR-C-09: CUSTOMER GET quotation when none exists → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-09: CUSTOMER GET /custom-requests/{id}/quotation when none exists returns 404")
    void customerToken_getQuotation_noQuotation_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomerEntity());
        when(quotationService.customerGetQuotation(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Quotation not found"));

        mockMvc.perform(get("/api/v1/custom-requests/10/quotation")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // CAR-C-10: CUSTOMER denied ADMIN review endpoint → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-10: CUSTOMER denied ADMIN review endpoint returns 403")
    void customerToken_adminReview_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/review")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CAR-C-11: ADMIN PATCH /admin/custom-requests/{id}/review → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-11: ADMIN PATCH /admin/custom-requests/{id}/review returns 200")
    void adminToken_reviewRequest_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildAdminEntity());
        when(customArtworkRequestService.adminReviewCustomRequest(any(), anyLong(), any()))
                .thenReturn(buildRequestResponse());

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/review")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR-C-12: ADMIN invalid transition → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-12: ADMIN review with invalid workflow transition returns 409")
    void adminToken_invalidTransition_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildAdminEntity());
        when(customArtworkRequestService.adminReviewCustomRequest(any(), anyLong(), any()))
                .thenThrow(new InvalidWorkflowTransitionException("Invalid transition"));

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");

        mockMvc.perform(patch("/api/v1/admin/custom-requests/10/review")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    // -------------------------------------------------------------------------
    // CAR-C-13: ADMIN POST /admin/custom-requests/{id}/quotation → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-13: ADMIN POST /admin/custom-requests/{id}/quotation returns 201")
    void adminToken_createQuotation_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildAdminEntity());
        when(quotationService.createQuotation(any(), anyLong(), any()))
                .thenReturn(buildQuotationResponse());

        QuotationCreateRequest createReq = new QuotationCreateRequest();
        createReq.setQuotedAmount(new BigDecimal("500.00"));
        createReq.setExpiryAt(OffsetDateTime.now().plusDays(7));

        mockMvc.perform(post("/api/v1/admin/custom-requests/10/quotation")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // CAR-C-14: ADMIN GET /admin/quotations/{id} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-14: ADMIN GET /admin/quotations/{id} returns 200")
    void adminToken_getQuotation_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(quotationService.adminGetQuotation(anyLong()))
                .thenReturn(buildQuotationResponse());

        mockMvc.perform(get("/api/v1/admin/quotations/100")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAR-C-15: Unauthenticated GET /custom-requests → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAR-C-15: Unauthenticated GET /custom-requests returns 401")
    void unauthenticated_listCustomRequests_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/custom-requests"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CustomArtworkRequestCreateRequest validCreateRequest() {
        CustomArtworkRequestCreateRequest req = new CustomArtworkRequestCreateRequest();
        req.setProductType("Oil Painting");
        req.setDescription("A custom oil painting of a landscape");
        return req;
    }
}
