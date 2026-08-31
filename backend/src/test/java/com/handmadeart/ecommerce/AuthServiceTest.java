package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.auth.LoginRequest;
import com.handmadeart.ecommerce.dto.auth.LoginResponse;
import com.handmadeart.ecommerce.dto.auth.RegisterRequest;
import com.handmadeart.ecommerce.dto.auth.UserResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.DuplicateEmailException;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the real {@link AuthService} production logic.
 *
 * Uses Mockito only — no Spring context, no database, no network.
 * Exercises the real service code directly, unlike AuthControllerTest
 * which mocks AuthService at the boundary.
 *
 * Covered:
 *   SVC-01  registration encodes the password before saving (not plaintext)
 *   SVC-02  stored password is verifiable but not equal to plaintext
 *   SVC-03  public registration always assigns CUSTOMER role
 *   SVC-04  duplicate email is rejected before any save is attempted
 *   SVC-05  successful login with correct credentials returns LoginResponse with token
 *   SVC-06  wrong password causes the service to propagate BadCredentialsException
 *   SVC-07  unknown email produces the same exception type as wrong password (no leakage)
 *   SVC-08  repository lookup uses case-insensitive email consistently
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Use the real BCryptPasswordEncoder so hashing assertions are genuine
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    // -------------------------------------------------------------------------
    // Registration tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SVC-01: register() hashes the password before calling repository.save()")
    void register_hashesPasswordBeforeSave() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        AppUser savedCapture = stubSaveReturning("alice@example.com", "Alice", null);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser arg = inv.getArgument(0);
            // Capture a reference so we can inspect it
            return arg; // return the same object (id will be null, that's fine for this test)
        });

        RegisterRequest req = buildRegisterRequest("Alice", "alice@example.com", "SecurePass1!");
        authService.register(req);

        // Capture what was actually passed to save()
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser persisted = captor.getValue();

        // The stored passwordHash must NOT be the plaintext
        assertThat(persisted.getPasswordHash())
                .as("Password must not be stored as plaintext")
                .isNotEqualTo("SecurePass1!");

        // The stored hash must be a valid BCrypt hash that verifies the plaintext
        assertThat(passwordEncoder.matches("SecurePass1!", persisted.getPasswordHash()))
                .as("Stored hash must verify the original plaintext")
                .isTrue();
    }

    @Test
    @DisplayName("SVC-02: Stored password hash does not equal plaintext and verifies with BCrypt")
    void register_storedHashIsNotPlaintextAndVerifies() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        String plaintext = "MyP@ssword99";
        RegisterRequest req = buildRegisterRequest("Dave", "dave@example.com", plaintext);
        authService.register(req);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();

        assertThat(storedHash).isNotEqualTo(plaintext);
        assertThat(storedHash).startsWith("$2a$") // BCrypt prefix
                .as("Stored value must be a BCrypt hash");
        assertThat(passwordEncoder.matches(plaintext, storedHash)).isTrue();
        assertThat(passwordEncoder.matches("wrongpassword", storedHash)).isFalse();
    }

    @Test
    @DisplayName("SVC-03: register() always assigns CUSTOMER role regardless of input")
    void register_alwaysAssignsCustomerRole() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = buildRegisterRequest("Eve", "eve@example.com", "Password1!");
        authService.register(req);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getRole())
                .as("Role must always be CUSTOMER for public registration")
                .isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("SVC-04: register() rejects duplicate email before attempting to save")
    void register_duplicateEmail_throwsAndNeverSaves() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        RegisterRequest req = buildRegisterRequest("Alice", "alice@example.com", "Password1!");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already exists");

        // repository.save() must never be called when the email is a duplicate
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Login tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SVC-05: login() with correct credentials returns LoginResponse containing a token")
    void login_correctCredentials_returnsLoginResponse() {
        AppUser user = buildAppUser(1L, "Alice", "alice@example.com", "hashedpw", UserRole.CUSTOMER);
        Authentication auth = new UsernamePasswordAuthenticationToken("alice@example.com", null);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice@example.com", "CUSTOMER")).thenReturn("mock.jwt.token");

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("SecurePass1!");

        LoginResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("SVC-06: login() with wrong password propagates BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("SVC-07: login() with unknown email throws the same BadCredentialsException as wrong password")
    void login_unknownEmail_throwsSameExceptionAsWrongPassword() {
        // AuthenticationManager raises BadCredentialsException for both unknown email and wrong
        // password — the service must not differentiate between them (no information leakage).
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@example.com");
        req.setPassword("AnyPassword!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // -------------------------------------------------------------------------
    // Case-insensitive email lookup
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SVC-08: register() uses existsByEmailIgnoreCase for the duplicate check")
    void register_usesCaseInsensitiveEmailCheck() {
        when(userRepository.existsByEmailIgnoreCase("ALICE@EXAMPLE.COM")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = buildRegisterRequest("Alice", "ALICE@EXAMPLE.COM", "Password1!");
        authService.register(req);

        // Confirm existsByEmailIgnoreCase (not existsByEmail) was the method called
        verify(userRepository).existsByEmailIgnoreCase("ALICE@EXAMPLE.COM");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RegisterRequest buildRegisterRequest(String name, String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private AppUser buildAppUser(Long id, String name, String email,
                                 String passwordHash, UserRole role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(name);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        try {
            var f = AppUser.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (Exception ignored) {
        }
        return user;
    }

    /** Convenience — not used by all tests, kept for potential future use. */
    @SuppressWarnings("unused")
    private AppUser stubSaveReturning(String email, String name, String passwordHash) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(name);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.CUSTOMER);
        return user;
    }
}
