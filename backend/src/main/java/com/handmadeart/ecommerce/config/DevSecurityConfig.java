package com.handmadeart.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY — Development Phase 1 security configuration.
 *
 * This configuration exists solely to allow the Spring application context to
 * load during Phase 1 initialization without a live PostgreSQL database or any
 * implemented authentication provider.
 *
 * IMPORTANT: This configuration must be replaced in Phase 3
 * (Authentication and Authorization) with the approved JWT-based security design.
 *
 * It must NOT be treated as a final security design.
 */
@Configuration
@EnableWebSecurity
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled for stateless REST API (will be stateless with JWT in Phase 3)
            .csrf(AbstractHttpConfigurer::disable)
            // All requests permitted temporarily so the context loads without auth infrastructure
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // Stateless session — JWT will enforce this properly in Phase 3
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable HTTP Basic to avoid auto-generated password noise in logs
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

}
