package com.handmadeart.ecommerce.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.AppUser;

/**
 * Response DTO for {@code POST /api/v1/auth/login} (200 OK).
 *
 * Contains the JWT access token and a safe current-user summary.
 * Approved shape: access_token, token_type, user summary (UC-002 / FR-AUTH-03,08).
 * Password hash is never included.
 */
public class LoginResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    private UserSummary user;

    public LoginResponse() {
    }

    public static LoginResponse of(String token, AppUser appUser) {
        LoginResponse response = new LoginResponse();
        response.accessToken = token;
        response.user = UserSummary.from(appUser);
        return response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserSummary getUser() {
        return user;
    }

    /**
     * Safe user summary included in the login response.
     * Never exposes password_hash.
     */
    public static class UserSummary {

        private Long id;
        private String name;
        private String email;
        private String role;

        private UserSummary() {
        }

        public static UserSummary from(AppUser user) {
            UserSummary s = new UserSummary();
            s.id = user.getId();
            s.name = user.getFullName();
            s.email = user.getEmail();
            s.role = user.getRole().name();
            return s;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}
