package com.handmadeart.ecommerce.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.AppUser;

import java.time.OffsetDateTime;

/**
 * Response DTO for registration (201) and {@code GET /api/v1/auth/me} (200).
 *
 * Never exposes password_hash or internal security fields.
 * Approved shape: id, name, email, phone, role, created_at (UC-001 / FR-AUTH-01,06).
 */
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private String phone;

    private String role;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public UserResponse() {
    }

    /**
     * Map from an {@link AppUser} entity to this response DTO.
     * Password hash is deliberately excluded.
     */
    public static UserResponse from(AppUser user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.name = user.getFullName();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.role = user.getRole().name();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
