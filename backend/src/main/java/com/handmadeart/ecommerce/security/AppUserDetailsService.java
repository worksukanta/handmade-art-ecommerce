package com.handmadeart.ecommerce.security;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation that loads users from the
 * {@code app_user} table via {@link AppUserRepository}.
 *
 * Email lookup is case-insensitive, consistent with the lower(email) uniqueness index
 * defined in the Flyway V2 migration (FR-AUTH-03).
 *
 * The loaded {@link UserDetails} carries a single authority prefixed with {@code ROLE_}
 * (Spring Security convention) derived from the stored {@link com.handmadeart.ecommerce.entity.UserRole}.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for the supplied credentials"));

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())))
                .build();
    }
}
