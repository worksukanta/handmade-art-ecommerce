package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AccountController;
import com.handmadeart.ecommerce.dto.account.AddressRequest;
import com.handmadeart.ecommerce.dto.account.AddressResponse;
import com.handmadeart.ecommerce.dto.account.ProfileResponse;
import com.handmadeart.ecommerce.dto.account.UpdateProfileRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AccountService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc controller tests for account profile and address endpoints.
 *
 * Covered:
 *   ACC-C-01  Unauthenticated GET /account/profile → 401
 *   ACC-C-02  CUSTOMER GET /account/profile → 200 + ProfileResponse
 *   ACC-C-03  CUSTOMER PUT /account/profile with valid body → 200 + ProfileResponse
 *   ACC-C-04  CUSTOMER PUT /account/profile with missing name → 400
 *   ACC-C-05  ADMIN GET /account/profile → 403 (CUSTOMER only)
 *   ACC-C-06  Unauthenticated GET /account/addresses → 401
 *   ACC-C-07  CUSTOMER GET /account/addresses → 200 + list
 *   ACC-C-08  CUSTOMER POST /account/addresses with valid body → 201 + AddressResponse
 *   ACC-C-09  CUSTOMER POST /account/addresses with missing required field → 400
 *   ACC-C-10  CUSTOMER PUT /account/addresses/{id} own address → 200 + AddressResponse
 *   ACC-C-11  CUSTOMER PUT /account/addresses/{id} foreign address → 404
 *   ACC-C-12  CUSTOMER DELETE /account/addresses/{id} own address → 204
 *   ACC-C-13  CUSTOMER DELETE /account/addresses/{id} foreign address → 404
 *   ACC-C-14  Unauthenticated DELETE /account/addresses/{id} → 401
 */
@WebMvcTest(AccountController.class)
@Import({
        AccountControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AccountService accountService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AppUserDetailsService appUserDetailsService;
    @MockitoBean private AuthService authService;
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

    // -------------------------------------------------------------------------
    // Helpers
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

    private AppUser buildCustomer() {
        AppUser user = new AppUser();
        user.setEmail("customer@example.com");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private ProfileResponse buildProfileResponse() {
        ProfileResponse r = new ProfileResponse();
        return r;
    }

    private AddressResponse buildAddressResponse() {
        AddressResponse r = new AddressResponse();
        return r;
    }

    private AddressRequest validAddressRequest() {
        AddressRequest req = new AddressRequest();
        req.setRecipientName("Jane Doe");
        req.setLine1("123 Main St");
        req.setCity("London");
        req.setStateProvince("England");
        req.setPostalCode("SW1A 1AA");
        req.setCountry("UK");
        return req;
    }

    // -------------------------------------------------------------------------
    // ACC-C-01: Unauthenticated GET /account/profile → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-01: Unauthenticated GET /account/profile returns 401")
    void unauthenticated_getProfile_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/account/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // ACC-C-02: CUSTOMER GET /account/profile → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-02: CUSTOMER GET /account/profile returns 200 + ProfileResponse")
    void customerToken_getProfile_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.getProfile(any())).thenReturn(buildProfileResponse());

        mockMvc.perform(get("/api/v1/account/profile")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ACC-C-03: CUSTOMER PUT /account/profile valid → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-03: CUSTOMER PUT /account/profile with valid body returns 200")
    void customerToken_putProfile_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.updateProfile(any(), any())).thenReturn(buildProfileResponse());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Updated Name");
        req.setPhone("07700900000");

        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ACC-C-04: PUT /account/profile missing name → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-04: CUSTOMER PUT /account/profile with blank name returns 400")
    void customerToken_putProfile_missingName_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        UpdateProfileRequest req = new UpdateProfileRequest();
        // name is blank — required field
        req.setName("");

        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // ACC-C-05: ADMIN GET /account/profile → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-05: ADMIN GET /account/profile returns 403")
    void adminToken_getProfile_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        mockMvc.perform(get("/api/v1/account/profile")
                        .header("Authorization", adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // ACC-C-06: Unauthenticated GET /account/addresses → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-06: Unauthenticated GET /account/addresses returns 401")
    void unauthenticated_listAddresses_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/account/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // ACC-C-07: CUSTOMER GET /account/addresses → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-07: CUSTOMER GET /account/addresses returns 200 + list")
    void customerToken_listAddresses_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.listAddresses(any())).thenReturn(List.of(buildAddressResponse()));

        mockMvc.perform(get("/api/v1/account/addresses")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // -------------------------------------------------------------------------
    // ACC-C-08: CUSTOMER POST /account/addresses valid → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-08: CUSTOMER POST /account/addresses with valid body returns 201")
    void customerToken_createAddress_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.createAddress(any(), any())).thenReturn(buildAddressResponse());

        mockMvc.perform(post("/api/v1/account/addresses")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddressRequest())))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // ACC-C-09: POST /account/addresses missing required field → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-09: CUSTOMER POST /account/addresses with missing city returns 400")
    void customerToken_createAddress_missingCity_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());

        AddressRequest req = validAddressRequest();
        req.setCity(""); // blank — required

        mockMvc.perform(post("/api/v1/account/addresses")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // ACC-C-10: CUSTOMER PUT /account/addresses/{id} own address → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-10: CUSTOMER PUT /account/addresses/{id} own address returns 200")
    void customerToken_updateAddress_ownAddress_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.updateAddress(any(), anyLong(), any())).thenReturn(buildAddressResponse());

        mockMvc.perform(put("/api/v1/account/addresses/1")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddressRequest())))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ACC-C-11: CUSTOMER PUT /account/addresses/{id} foreign address → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-11: CUSTOMER PUT /account/addresses/{id} foreign address returns 404")
    void customerToken_updateAddress_foreignAddress_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        when(accountService.updateAddress(any(), anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("Address not found"));

        mockMvc.perform(put("/api/v1/account/addresses/999")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddressRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ACC-C-12: CUSTOMER DELETE /account/addresses/{id} own address → 204
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-12: CUSTOMER DELETE /account/addresses/{id} own address returns 204")
    void customerToken_deleteAddress_ownAddress_returns204() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        doNothing().when(accountService).deleteAddress(any(), anyLong());

        mockMvc.perform(delete("/api/v1/account/addresses/1")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // ACC-C-13: CUSTOMER DELETE /account/addresses/{id} foreign → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-13: CUSTOMER DELETE /account/addresses/{id} foreign address returns 404")
    void customerToken_deleteAddress_foreignAddress_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser()).thenReturn(buildCustomer());
        doThrow(new ResourceNotFoundException("Address not found"))
                .when(accountService).deleteAddress(any(), anyLong());

        mockMvc.perform(delete("/api/v1/account/addresses/999")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ACC-C-14: Unauthenticated DELETE /account/addresses/{id} → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-C-14: Unauthenticated DELETE /account/addresses/{id} returns 401")
    void unauthenticated_deleteAddress_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/account/addresses/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
