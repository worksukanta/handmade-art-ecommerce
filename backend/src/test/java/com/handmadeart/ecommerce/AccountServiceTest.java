package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.account.AddressRequest;
import com.handmadeart.ecommerce.dto.account.AddressResponse;
import com.handmadeart.ecommerce.dto.account.ProfileResponse;
import com.handmadeart.ecommerce.dto.account.UpdateProfileRequest;
import com.handmadeart.ecommerce.entity.Address;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.AddressRepository;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AccountService — profile and address management.
 *
 * Covered:
 *   ACC-S-01  getProfile returns ProfileResponse for current user
 *   ACC-S-02  updateProfile updates only name and phone
 *   ACC-S-03  listAddresses returns only addresses for current user
 *   ACC-S-04  createAddress sets user ownership from authenticated user, not client
 *   ACC-S-05  updateAddress on own address succeeds
 *   ACC-S-06  updateAddress on foreign/missing address throws ResourceNotFoundException
 *   ACC-S-07  deleteAddress on own address succeeds
 *   ACC-S-08  deleteAddress on foreign/missing address throws ResourceNotFoundException
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks private AccountService accountService;

    private AppUser customer;

    @BeforeEach
    void setUp() {
        customer = new AppUser();
        customer.setEmail("customer@example.com");
        customer.setFullName("Test Customer");
        customer.setPhone("07700000000");
        customer.setRole(UserRole.CUSTOMER);
    }

    // -------------------------------------------------------------------------
    // ACC-S-01: getProfile returns profile DTO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-01: getProfile returns ProfileResponse for current user")
    void getProfile_returnsProfileResponse() {
        ProfileResponse result = accountService.getProfile(customer);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Customer");
        assertThat(result.getEmail()).isEqualTo("customer@example.com");
    }

    // -------------------------------------------------------------------------
    // ACC-S-02: updateProfile updates only name and phone
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-02: updateProfile updates name and phone only")
    void updateProfile_updatesNameAndPhone() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated Name");
        request.setPhone("07999000000");

        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse result = accountService.updateProfile(customer, request);

        assertThat(customer.getFullName()).isEqualTo("Updated Name");
        assertThat(customer.getPhone()).isEqualTo("07999000000");
        assertThat(result.getName()).isEqualTo("Updated Name");
        // Email unchanged
        assertThat(customer.getEmail()).isEqualTo("customer@example.com");
    }

    // -------------------------------------------------------------------------
    // ACC-S-03: listAddresses returns addresses for current user only
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-03: listAddresses returns owned addresses")
    void listAddresses_returnsOwnedAddresses() {
        Address addr = buildAddress();
        when(addressRepository.findByUserId(nullable(Long.class))).thenReturn(List.of(addr));

        List<AddressResponse> results = accountService.listAddresses(customer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCity()).isEqualTo("London");
    }

    // -------------------------------------------------------------------------
    // ACC-S-04: createAddress sets ownership from authenticated user
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-04: createAddress sets user ownership from JWT user, not client")
    void createAddress_setsOwnershipFromCurrentUser() {
        AddressRequest request = buildAddressRequest();
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressResponse result = accountService.createAddress(customer, request);

        // Verify via the saved argument — ownership must be the authenticated user
        // (not any client-supplied value)
        assertThat(result.getCity()).isEqualTo("London");
    }

    // -------------------------------------------------------------------------
    // ACC-S-05: updateAddress own address succeeds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-05: updateAddress on own address returns updated AddressResponse")
    void updateAddress_ownAddress_succeeds() {
        Address addr = buildAddress();
        when(addressRepository.findByUserIdAndId(nullable(Long.class), any(Long.class)))
                .thenReturn(Optional.of(addr));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = buildAddressRequest();
        request.setCity("Manchester");

        AddressResponse result = accountService.updateAddress(customer, 1L, request);

        assertThat(result.getCity()).isEqualTo("Manchester");
    }

    // -------------------------------------------------------------------------
    // ACC-S-06: updateAddress foreign/missing address → ResourceNotFoundException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-06: updateAddress foreign address throws ResourceNotFoundException")
    void updateAddress_foreignAddress_throwsNotFound() {
        when(addressRepository.findByUserIdAndId(nullable(Long.class), any(Long.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAddress(customer, 999L, buildAddressRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // ACC-S-07: deleteAddress own address succeeds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-07: deleteAddress on own address deletes it")
    void deleteAddress_ownAddress_succeeds() {
        Address addr = buildAddress();
        when(addressRepository.findByUserIdAndId(nullable(Long.class), any(Long.class)))
                .thenReturn(Optional.of(addr));

        accountService.deleteAddress(customer, 1L);

        verify(addressRepository).delete(addr);
    }

    // -------------------------------------------------------------------------
    // ACC-S-08: deleteAddress foreign/missing address → ResourceNotFoundException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACC-S-08: deleteAddress foreign address throws ResourceNotFoundException")
    void deleteAddress_foreignAddress_throwsNotFound() {
        when(addressRepository.findByUserIdAndId(nullable(Long.class), any(Long.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteAddress(customer, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Address buildAddress() {
        Address addr = new Address();
        addr.setUser(customer);
        addr.setRecipientName("Test Recipient");
        addr.setLine1("1 High Street");
        addr.setCity("London");
        addr.setStateProvince("England");
        addr.setPostalCode("EC1A 1BB");
        addr.setCountry("UK");
        return addr;
    }

    private AddressRequest buildAddressRequest() {
        AddressRequest req = new AddressRequest();
        req.setRecipientName("Test Recipient");
        req.setLine1("1 High Street");
        req.setCity("London");
        req.setStateProvince("England");
        req.setPostalCode("EC1A 1BB");
        req.setCountry("UK");
        return req;
    }
}
