package com.handmadeart.ecommerce.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for POST /api/v1/account/addresses (create) and
 * PUT /api/v1/account/addresses/{id} (update).
 *
 * The user_id is never accepted from the client — ownership is established
 * from the authenticated JWT principal only.
 *
 * DEC-010 DEFERRED: isDefault field is accepted if supplied but no automatic
 * default-promotion logic is applied; only the documented DB unique constraint
 * (one default per user via partial unique index) is enforced at the DB layer.
 */
public class AddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must not exceed 150 characters")
    @JsonProperty("recipient_name")
    private String recipientName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Line 1 must not exceed 255 characters")
    private String line1;

    @Size(max = 255, message = "Line 2 must not exceed 255 characters")
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State/province is required")
    @Size(max = 100, message = "State/province must not exceed 100 characters")
    @JsonProperty("state_province")
    private String stateProvince;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @JsonProperty("postal_code")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @JsonProperty("is_default")
    private boolean isDefault = false;

    public AddressRequest() {
    }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }

    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateProvince() { return stateProvince; }
    public void setStateProvince(String stateProvince) { this.stateProvince = stateProvince; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
