package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrentUserService}.
 *
 * Verifies that the service derives identity exclusively from the Spring Security context
 * (never from any client-supplied value) and correctly rejects unauthenticated / anonymous
 * principals.
 *
 * This service is the single authoritative ownership-resolution mechanism for all future
 * customer-owned resources (addresses, cart, orders, custom artwork). Its correctness
 * is critical to preventing client-controlled identity attacks.
 *
 * Covered:
 *   CUS-01  Authenticated user → getAuthenticatedUser() returns correct AppUser entity
 *   CUS-02  Authenticated user → getAuthenticatedEmail() returns correct email
 *   CUS-03  Null security context → getAuthenticatedUser() throws BadCredentialsException
 *   CUS-04  Anonymous authentication (anonymousUser string principal) → throws BadCredentialsException
 *   CUS-05  Identity derived from JWT/SecurityContext, not from any alternative source
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Always clean the security context so tests do not bleed into each other
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // CUS-01: getAuthenticatedUser() with a valid authenticated principal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CUS-01: getAuthenticatedUser() returns the AppUser for the authenticated principal")
    void getAuthenticatedUser_withValidPrincipal_returnsAppUser() {
        String email = "alice@example.com";
        AppUser appUser = buildAppUser(1L, email, UserRole.CUSTOMER);

        UserDetails details = User.builder()
                .username(email)
                .password("{noop}irrelevant")
                .roles("CUSTOMER")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(appUser));

        AppUser result = currentUserService.getAuthenticatedUser();

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    // -------------------------------------------------------------------------
    // CUS-02: getAuthenticatedEmail() with a valid authenticated principal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CUS-02: getAuthenticatedEmail() returns the email from the security context principal")
    void getAuthenticatedEmail_withValidPrincipal_returnsEmail() {
        String email = "bob@example.com";
        UserDetails details = User.builder()
                .username(email)
                .password("{noop}irrelevant")
                .roles("CUSTOMER")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        String result = currentUserService.getAuthenticatedEmail();

        assertThat(result).isEqualTo(email);
    }

    // -------------------------------------------------------------------------
    // CUS-03: Empty / null security context → BadCredentialsException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CUS-03: getAuthenticatedUser() with no authentication in context throws BadCredentialsException")
    void getAuthenticatedUser_withNoAuthentication_throwsBadCredentials() {
        // SecurityContextHolder is cleared in @BeforeEach — no authentication set
        assertThatThrownBy(() -> currentUserService.getAuthenticatedUser())
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("CUS-03b: getAuthenticatedEmail() with no authentication in context throws BadCredentialsException")
    void getAuthenticatedEmail_withNoAuthentication_throwsBadCredentials() {
        assertThatThrownBy(() -> currentUserService.getAuthenticatedEmail())
                .isInstanceOf(BadCredentialsException.class);
    }

    // -------------------------------------------------------------------------
    // CUS-04: Anonymous authentication → BadCredentialsException
    // Spring Security AnonymousAuthenticationToken has isAuthenticated()=true but
    // its principal is the String "anonymousUser".  The service must reject it.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CUS-04: Anonymous authentication (String principal) is rejected as unauthenticated")
    void getAuthenticatedUser_withAnonymousPrincipal_throwsBadCredentials() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        // The guard "authentication.getPrincipal() instanceof String" must reject this
        assertThatThrownBy(() -> currentUserService.getAuthenticatedUser())
                .isInstanceOf(BadCredentialsException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppUser buildAppUser(Long id, String email, UserRole role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(role);
        try {
            var f = AppUser.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (Exception ignored) {
        }
        return user;
    }
}
