package com.handmadeart.ecommerce.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.handmadeart.ecommerce.entity.Address;

import java.time.OffsetDateTime;

/**
 * Response DTO for address list/create/update operations.
 *
 * REST API Spec §3 (Account / Address endpoints).
 * Never exposes the user_id (ownership is implicit from authentication).
 */
public class AddressResponse {

    private Long id;

    @JsonProperty("recipient_name")
    private String recipientName;

    private String line1;
    private String line2;
    private String city;

    @JsonProperty("state_province")
    private String stateProvince;

    @JsonProperty("postal_code")
    private String postalCode;

    private String country;
    private String phone;

    @JsonProperty("is_default")
    private boolean isDefault;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public AddressResponse() {
    }

    public static AddressResponse from(Address address) {
        AddressResponse dto = new AddressResponse();
        dto.id = address.getId();
        dto.recipientName = address.getRecipientName();
        dto.line1 = address.getLine1();
        dto.line2 = address.getLine2();
        dto.city = address.getCity();
        dto.stateProvince = address.getStateProvince();
        dto.postalCode = address.getPostalCode();
        dto.country = address.getCountry();
        dto.phone = address.getPhone();
        dto.isDefault = address.isDefault();
        dto.createdAt = address.getCreatedAt();
        dto.updatedAt = address.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getRecipientName() { return recipientName; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getStateProvince() { return stateProvince; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public String getPhone() { return phone; }
    public boolean isDefault() { return isDefault; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
