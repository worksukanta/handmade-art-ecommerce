package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.account.AddressRequest;
import com.handmadeart.ecommerce.dto.account.AddressResponse;
import com.handmadeart.ecommerce.dto.account.ProfileResponse;
import com.handmadeart.ecommerce.dto.account.UpdateProfileRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.AccountService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer account controller — profile and address management.
 *
 * Endpoints (REST API Spec §3):
 *   GET    /api/v1/account/profile              — read own profile
 *   PUT    /api/v1/account/profile              — update own profile (name, phone only)
 *   GET    /api/v1/account/addresses            — list own addresses
 *   POST   /api/v1/account/addresses            — create address
 *   PUT    /api/v1/account/addresses/{id}       — update own address
 *   DELETE /api/v1/account/addresses/{id}       — delete own address
 *
 * Authorization:
 *   All endpoints require CUSTOMER role (SecurityConfig).
 *   Identity is resolved from JWT via CurrentUserService; no client-supplied
 *   user IDs are accepted.
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;

    public AccountController(AccountService accountService,
                             CurrentUserService currentUserService) {
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    // =========================================================================
    // Profile
    // =========================================================================

    /**
     * GET /api/v1/account/profile — return authenticated customer's profile.
     *
     * Auth:    CUSTOMER
     * Success: 200 + ProfileResponse
     * Errors:  401
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        return ResponseEntity.ok(accountService.getProfile(currentUser));
    }

    /**
     * PUT /api/v1/account/profile — update customer-editable profile fields.
     *
     * Auth:    CUSTOMER
     * Request: UpdateProfileRequest {name, phone}
     * Success: 200 + ProfileResponse
     * Errors:  400 validation, 401
     */
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        return ResponseEntity.ok(accountService.updateProfile(currentUser, request));
    }

    // =========================================================================
    // Addresses
    // =========================================================================

    /**
     * GET /api/v1/account/addresses — list all own addresses.
     *
     * Auth:    CUSTOMER
     * Success: 200 + List&lt;AddressResponse&gt;
     * Errors:  401
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses() {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        return ResponseEntity.ok(accountService.listAddresses(currentUser));
    }

    /**
     * POST /api/v1/account/addresses — create a new address.
     *
     * Auth:    CUSTOMER
     * Request: AddressRequest (all required fields)
     * Success: 201 + AddressResponse
     * Errors:  400 validation, 401
     */
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        AddressResponse response = accountService.createAddress(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/account/addresses/{id} — update an owned address.
     *
     * Auth:    CUSTOMER
     * Request: AddressRequest (all fields)
     * Success: 200 + AddressResponse
     * Errors:  400 validation, 401, 404 not owned
     */
    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        return ResponseEntity.ok(accountService.updateAddress(currentUser, id, request));
    }

    /**
     * DELETE /api/v1/account/addresses/{id} — delete an owned address.
     *
     * Auth:    CUSTOMER
     * Success: 204 No Content
     * Errors:  401, 404 not owned
     */
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        accountService.deleteAddress(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
