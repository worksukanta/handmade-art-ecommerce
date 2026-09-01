package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Resolves the authenticated {@link AppUser} from the Spring Security context.
 *
 * This service is the single authoritative way for feature services to obtain
 * the currently authenticated user entity. It:
 *   - derives identity exclusively from the validated JWT (via SecurityContextHolder),
 *     never from any client-supplied identifier;
 *   - uses case-insensitive email lookup (consistent with lower(email) DB index);
 *   - fails safely with an exception when no valid authentication exists.
 *
 * Usage in future feature services (SDD §8.6, REST API Spec §2):
 * <pre>
 *   AppUser currentUser = currentUserService.getAuthenticatedUser();
 *   // then enforce ownership: currentUser.getId().equals(resource.getOwnerId())
 * </pre>
 *
 * Password hashes are never exposed through this service.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public CurrentUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Return the {@link AppUser} entity for the currently authenticated principal.
     *
     * The identity is taken from {@link SecurityContextHolder} — populated by
     * {@link com.handmadeart.ecommerce.security.JwtAuthenticationFilter} after
     * validating the Bearer token. Client-supplied user IDs are never consulted.
     *
     * @return the authenticated AppUser
     * @throws org.springframework.security.core.AuthenticationException if no
     *         valid authentication is present in the security context
     */
    public AppUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            throw new BadCredentialsException("No authenticated user in security context");
        }

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Authenticated user account not found"));
    }

    /**
     * Return the email of the currently authenticated principal without loading the entity.
     * Useful for logging or lightweight identity checks.
     *
     * @return the authenticated user's email
     * @throws org.springframework.security.core.AuthenticationException if no
     *         valid authentication is present
     */
    public String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            throw new BadCredentialsException("No authenticated user in security context");
        }

        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
