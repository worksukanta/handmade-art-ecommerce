package com.handmadeart.ecommerce.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for PUT /api/v1/account/profile.
 *
 * Only customer-editable fields are accepted: name, phone.
 * Email, role, password, id, and timestamps are NOT editable through this endpoint.
 * Password change is not part of this endpoint.
 */
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String name;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    public UpdateProfileRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
