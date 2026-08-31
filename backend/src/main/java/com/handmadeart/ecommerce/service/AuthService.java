package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.auth.LoginRequest;
import com.handmadeart.ecommerce.dto.auth.LoginResponse;
import com.handmadeart.ecommerce.dto.auth.RegisterRequest;
import com.handmadeart.ecommerce.dto.auth.UserResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.DuplicateEmailException;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service for registration, login, and current-user resolution.
 *
 * Design constraints (SDD §8, REST API Spec §4):
 * - Public registration always creates CUSTOMER role; role is never accepted from input.
 * - Password is BCrypt-hashed before persistence; plaintext never stored/logged.
 * - Email uniqueness check is case-insensitive, consistent with lower(email) index.
 * - Login failure returns a generic BadCredentialsException — no email-existence leakage.
 * - JWT carries email + role; no password or internal identifiers exposed in the token.
 *
 * DEC-002 (JWT logout/revocation) — OPEN. Logout is not implemented here.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new CUSTOMER account.
     *
     * @param request validated registration fields
     * @return a safe UserResponse (no password data)
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email address already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        user.setFullName(request.getName());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Public registration always creates CUSTOMER — role is never accepted from input
        user.setRole(UserRole.CUSTOMER);

        AppUser saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    /**
     * Authenticate a user and return a JWT + safe user summary.
     *
     * Spring's AuthenticationManager validates the password against the stored BCrypt hash.
     * On failure it throws BadCredentialsException — no information about whether the email exists.
     *
     * @param request email + password credentials
     * @return LoginResponse containing the JWT access token and user summary
     */
    public LoginResponse login(LoginRequest request) {
        // Delegate credential verification to Spring Security's DaoAuthenticationProvider
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Load the full entity to build the response (authenticated principal holds email)
        AppUser user = userRepository.findByEmailIgnoreCase(
                authentication.getName()).orElseThrow();

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return LoginResponse.of(token, user);
    }

    /**
     * Return the safe profile for the currently authenticated user.
     *
     * @param email the authenticated user's email (from the validated JWT subject)
     * @return UserResponse for the resolved account
     */
    public UserResponse getCurrentUser(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Authenticated user not found"));
        return UserResponse.from(user);
    }
}
