package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.auth.LoginRequest;
import com.handmadeart.ecommerce.dto.auth.LoginResponse;
import com.handmadeart.ecommerce.dto.auth.RegisterRequest;
import com.handmadeart.ecommerce.dto.auth.UserResponse;
import com.handmadeart.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication REST controller.
 *
 * Endpoints (REST API Specification §4):
 *   POST /api/v1/auth/register — public; returns 201 + UserResponse
 *   POST /api/v1/auth/login    — public; returns 200 + LoginResponse
 *   GET  /api/v1/auth/me       — authenticated; returns 200 + UserResponse
 *
 * Note: POST /api/v1/auth/logout is excluded from this phase (DEC-002 OPEN).
 *
 * Controllers are thin — no business logic here.
 * Identity for /me is derived from the validated JWT, never from a client-supplied ID.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new customer account.
     *
     * Method:  POST
     * Path:    /api/v1/auth/register
     * Auth:    Public
     * Success: 201 Created + UserResponse (no password data)
     * Errors:  400 validation, 409 duplicate email
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate and receive a JWT access token.
     *
     * Method:  POST
     * Path:    /api/v1/auth/login
     * Auth:    Public
     * Success: 200 OK + LoginResponse (access token + user summary)
     * Errors:  400 malformed, 401 invalid credentials
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Return the safe profile of the currently authenticated user.
     *
     * Method:  GET
     * Path:    /api/v1/auth/me
     * Auth:    Required (Bearer token)
     * Success: 200 OK + UserResponse
     * Errors:  401 unauthenticated
     *
     * Identity is derived from the validated JWT — client-supplied user IDs are never trusted.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        UserResponse response = authService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(response);
    }
}
