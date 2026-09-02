package com.handmadeart.ecommerce.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.AppUser;

import java.time.OffsetDateTime;

/**
 * Admin customer detail/list response DTO.
 *
 * REST API Spec §16: GET /api/v1/admin/customers and GET /api/v1/admin/customers/{id}.
 *
 * Read-only. Never exposes password_hash or security credentials.
 * Role is included for admin visibility.
 */
public class AdminCustomerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public AdminCustomerResponse() {
    }

    public static AdminCustomerResponse from(AppUser user) {
        AdminCustomerResponse dto = new AdminCustomerResponse();
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
