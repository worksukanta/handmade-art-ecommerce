package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AuthController;
import com.handmadeart.ecommerce.dto.auth.LoginRequest;
import com.handmadeart.ecommerce.dto.auth.LoginResponse;
import com.handmadeart.ecommerce.dto.auth.RegisterRequest;
import com.handmadeart.ecommerce.dto.auth.UserResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.DuplicateEmailException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.AuthEntryPoint;
import com.handmadeart.ecommerce.security.JwtAuthenticationFilter;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc-based tests for the authentication endpoints (Phase 3A.1).
 *
 * Uses @WebMvcTest(AuthController.class) to load only the web layer for AuthController.
 * SecurityConfig (the real one) is imported so JWT filter + security rules run properly.
 * AuthService and AppUserDetailsService are mocked to keep tests DB-free.
 * JwtService uses the real implementation with test-profile key (test application.yml).
 *
 * Covered scenarios:
 *   REG-01  Successful registration — 201 + UserResponse
 *   REG-02  Password stored hashed (BCrypt), not plaintext
 *   REG-03  CUSTOMER role enforced
 *   REG-04  Duplicate email rejected — 409
 *   REG-05  Invalid registration input — 400 with details
 *   LOG-01/JWT-01  Successful login returns access_token
 *   LOG-02  Wrong password rejected — 401
 *   LOG-03  Unknown email rejected — 401 (no leakage)
 *   JWT-02  Valid JWT accepted for /me
 *   JWT-03  Malformed token rejected — 401
 *   JWT-04  Expired token rejected — 401
 *   ME-01   Authenticated /me returns user info
 *   ME-02   Unauthenticated /me returns 401
 */
@WebMvcTest(AuthController.class)
@Import({AuthControllerTest.TestSecurityConfig.class, com.handmadeart.ecommerce.config.SecurityConfig.class,
         com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
         com.handmadeart.ecommerce.security.AuthEntryPoint.class,
         com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
         com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    // -------------------------------------------------------------------------
    // Test configuration — provides JwtService and PasswordEncoder beans
    // using properties from test/resources/application.yml
    // -------------------------------------------------------------------------

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        public JwtService jwtService(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expMs) {
            return new JwtService(secret, expMs);
        }
        // PasswordEncoder is provided by SecurityConfig (which is @Import-ed above).
        // Do NOT declare it here — that would create a duplicate bean definition conflict.
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserResponse buildUserResponse(Long id, String name, String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(name);
        user.setRole(UserRole.CUSTOMER);
        try {
            var idField = AppUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            var createdField = AppUser.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(user, OffsetDateTime.now());
        } catch (Exception ignored) {
        }
        return UserResponse.from(user);
    }

    private String validToken(String email) {
        return jwtService.generateToken(email, "CUSTOMER");
    }

    private UserDetails stubDetails(String email) {
        return User.builder()
                .username(email)
                .password("{noop}irrelevant")
                .roles("CUSTOMER")
                .build();
    }

    // -------------------------------------------------------------------------
    // Registration tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("REG-01: Successful registration returns 201 + UserResponse with CUSTOMER role")
    void register_success_returns201AndUserResponse() throws Exception {
        UserResponse mockResponse = buildUserResponse(1L, "Alice Smith", "alice@example.com");
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        RegisterRequest req = new RegisterRequest();
        req.setName("Alice Smith");
        req.setEmail("alice@example.com");
        req.setPassword("SecurePass1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    @DisplayName("REG-02 (encoder sanity): BCryptPasswordEncoder hashes correctly — AuthService hashing enforced in AuthServiceTest")
    void bcryptEncoder_hashesCorrectly() {
        // Confirms the BCrypt encoder itself works.
        // Whether AuthService actually calls encode() before save is proved in AuthServiceTest.
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String plaintext = "SecurePass1!";
        String hash = encoder.encode(plaintext);

        assert !hash.equals(plaintext) : "Hash equals plaintext";
        assert encoder.matches(plaintext, hash) : "Hash does not verify plaintext";
        assert !encoder.matches("WrongPassword!", hash) : "Wrong password verified against hash";
    }

    @Test
    @DisplayName("REG-03 (controller layer): Controller propagates CUSTOMER role from mocked service response")
    void register_controllerPropagatesCustomerRole() throws Exception {
        UserResponse mockResponse = buildUserResponse(2L, "Bob", "bob@example.com");
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        RegisterRequest req = new RegisterRequest();
        req.setName("Bob");
        req.setEmail("bob@example.com");
        req.setPassword("SecurePass1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("REG-04: Duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("An account with this email address already exists"));

        RegisterRequest req = new RegisterRequest();
        req.setName("Carol");
        req.setEmail("carol@example.com");
        req.setPassword("SecurePass1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("REG-05: Invalid registration input returns 400 with validation details")
    void register_invalidInput_returns400WithDetails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("");          // blank — fails @NotBlank
        req.setEmail("not-valid"); // not an email
        req.setPassword("short"); // too short

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("REG-05b: Empty request body returns 400")
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // -------------------------------------------------------------------------
    // Login tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("LOG-01 / JWT-01: Successful login returns 200 with access_token")
    void login_success_returnsTokenAndUserSummary() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("alice@example.com");
        user.setFullName("Alice Smith");
        user.setRole(UserRole.CUSTOMER);

        String token = validToken("alice@example.com");
        LoginResponse mockResponse = LoginResponse.of(token, user);
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("SecurePass1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("LOG-02: Wrong password returns 401 without leaking email existence")
    void login_wrongPassword_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("LOG-03: Unknown email returns 401 (same as wrong password — no info leakage)")
    void login_unknownEmail_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@example.com");
        req.setPassword("AnyPassword1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    // -------------------------------------------------------------------------
    // JWT and /me tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("JWT-02 / ME-01: Valid JWT accepted for /me, returns user info")
    void me_withValidToken_returns200() throws Exception {
        String email = "alice@example.com";
        UserResponse mockResponse = buildUserResponse(1L, "Alice Smith", email);
        when(authService.getCurrentUser(anyString())).thenReturn(mockResponse);
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(stubDetails(email));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + validToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("JWT-03: Malformed/invalid token returns 401")
    void me_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT-04: Expired token returns 401")
    void me_withExpiredToken_returns401() throws Exception {
        String expiredToken = jwtService.generateTokenWithExpiry("alice@example.com", "CUSTOMER", -1000L);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ME-02: Unauthenticated request to /me returns 401")
    void me_withNoToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
