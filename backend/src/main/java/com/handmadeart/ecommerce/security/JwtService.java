package com.handmadeart.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT generation and validation service.
 *
 * Externalized configuration:
 *   app.jwt.secret       — Base64-encoded HMAC-SHA256 signing key (min 256 bits).
 *                          Must be set via JWT_SECRET environment variable in production.
 *   app.jwt.expiration-ms — Token validity period in milliseconds (default: 24 h).
 *
 * Claims included:
 *   sub  — user email (stable identifier across sessions)
 *   role — user role (CUSTOMER / ADMIN) for downstream authorization
 *   iat  — issued-at (set by JJWT)
 *   exp  — expiry (set by JJWT)
 *
 * Secrets are never logged (SDD §15, NFR-17).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT for the given user.
     *
     * @param email     the user's email address (becomes the {@code sub} claim)
     * @param role      the user's role name (e.g. "CUSTOMER")
     * @return a compact, signed JWT string
     */
    public String generateToken(String email, String role) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the subject (email) from a validated JWT.
     *
     * @param token a compact JWT string
     * @return the email stored in the {@code sub} claim
     * @throws JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validate a token against the given UserDetails.
     * Checks signature, expiry, and that the subject matches.
     *
     * @param token       a compact JWT string
     * @param userDetails the user to validate against
     * @return {@code true} if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Generate a token with a custom expiration (used for testing expired-token scenarios).
     */
    public String generateTokenWithExpiry(String email, String role, long customExpirationMs) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + customExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }
}
