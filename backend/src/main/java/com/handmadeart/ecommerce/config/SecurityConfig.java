package com.handmadeart.ecommerce.config;

import com.handmadeart.ecommerce.security.ApiAccessDeniedHandler;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.AuthEntryPoint;
import com.handmadeart.ecommerce.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Production Spring Security configuration.
 *
 * Design (SDD §8, §14):
 * - Stateless JWT-based authentication; no HTTP sessions.
 * - CSRF disabled for stateless REST API (SDD §14 CSRF note).
 * - Route-level authorization rules:
 *     /api/v1/auth/register, /api/v1/auth/login — public (no auth required)
 *     /api/v1/admin/**                          — ADMIN role required
 *     all other requests                        — authenticated (any role)
 * - @EnableMethodSecurity enables @PreAuthorize for fine-grained method-level
 *   authorization inside service/controller methods (SDD §8.6).
 * - Unauthenticated requests → 401 JSON (AuthEntryPoint).
 * - Authenticated but insufficient role → 403 JSON (ApiAccessDeniedHandler).
 * - BCrypt PasswordEncoder with default strength (10 rounds).
 * - JWT filter runs before UsernamePasswordAuthenticationFilter.
 *
 * DEC-002 (logout/revocation) remains OPEN — logout is not implemented here.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthEntryPoint authEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(AppUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthFilter,
                          AuthEntryPoint authEntryPoint,
                          ApiAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // Public: registration and login require no authentication
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                // Admin-only: all /api/v1/admin/**  paths require the ADMIN role
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // All other requests require a valid authenticated principal (any role)
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider(PasswordEncoder) constructor preferred in Spring Security 6.3+
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
