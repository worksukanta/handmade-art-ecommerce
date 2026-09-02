package com.handmadeart.ecommerce.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.AppUser;

import java.time.OffsetDateTime;

/**
 * Response DTO for GET/PUT /api/v1/account/profile.
 *
 * Exposes customer-readable profile fields only.
 * Password hash, internal IDs beyond the user's own id, and role are
 * included for informational purposes but role is read-only.
 * Never exposes password_hash or security credentials.
 */
public class ProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public ProfileResponse() {
    }

    public static ProfileResponse from(AppUser user) {
        ProfileResponse dto = new ProfileResponse();
        dto.id = user.getId();
        dto.name = user.getFullName();
        dto.email = user.getEmail();
        dto.phone = user.getPhone();
        dto.role = user.getRole().name();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
