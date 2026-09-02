package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.account.AddressRequest;
import com.handmadeart.ecommerce.dto.account.AddressResponse;
import com.handmadeart.ecommerce.dto.account.ProfileResponse;
import com.handmadeart.ecommerce.dto.account.UpdateProfileRequest;
import com.handmadeart.ecommerce.entity.Address;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.AddressRepository;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for customer account profile and address management.
 *
 * Security invariants:
 *   - currentUser is ALWAYS supplied by the controller from the JWT-validated
 *     security context via CurrentUserService. Client-supplied user IDs are
 *     never accepted.
 *   - Address ownership is enforced by querying with (userId, addressId) so
 *     that a foreign address ID is indistinguishable from a missing one (404).
 *
 * Profile rules:
 *   - Only name and phone are customer-editable.
 *   - Email, role, password, id, timestamps are NOT editable through this service.
 *
 * Address rules (DEC-010 DEFERRED):
 *   - isDefault is persisted as supplied; no automatic default promotion logic.
 *   - The DB partial unique index enforces at-most-one-default-per-user.
 */
@Service
public class AccountService {

    private final AppUserRepository userRepository;
    private final AddressRepository addressRepository;

    public AccountService(AppUserRepository userRepository,
                          AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    // =========================================================================
    // Profile
    // =========================================================================

    /**
     * GET /api/v1/account/profile — return the authenticated customer's profile.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(AppUser currentUser) {
        return ProfileResponse.from(currentUser);
    }

    /**
     * PUT /api/v1/account/profile — update customer-editable profile fields.
     *
     * Only name and phone are updated; email, role, password, and timestamps
     * are protected and cannot be changed through this endpoint.
     */
    @Transactional
    public ProfileResponse updateProfile(AppUser currentUser, UpdateProfileRequest request) {
        currentUser.setFullName(request.getName());
        currentUser.setPhone(request.getPhone());
        AppUser saved = userRepository.save(currentUser);
        return ProfileResponse.from(saved);
    }

    // =========================================================================
    // Addresses
    // =========================================================================

    /**
     * GET /api/v1/account/addresses — list all addresses for the authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(AppUser currentUser) {
        return addressRepository.findByUserId(currentUser.getId())
                .stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * POST /api/v1/account/addresses — create a new address for the authenticated customer.
     *
     * Ownership is established from the authenticated user, never from the request body.
     */
    @Transactional
    public AddressResponse createAddress(AppUser currentUser, AddressRequest request) {
        Address address = new Address();
        address.setUser(currentUser);
        applyAddressFields(address, request);
        Address saved = addressRepository.save(address);
        return AddressResponse.from(saved);
    }

    /**
     * PUT /api/v1/account/addresses/{id} — update an owned address.
     *
     * Querying with (userId, addressId) ensures a foreign address ID results in
     * 404 (same non-disclosure as other owned resources).
     */
    @Transactional
    public AddressResponse updateAddress(AppUser currentUser, Long addressId, AddressRequest request) {
        Address address = addressRepository
                .findByUserIdAndId(currentUser.getId(), addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        applyAddressFields(address, request);
        Address saved = addressRepository.save(address);
        return AddressResponse.from(saved);
    }

    /**
     * DELETE /api/v1/account/addresses/{id} — delete an owned address.
     *
     * Foreign address ID → 404 (same non-disclosure semantics).
     */
    @Transactional
    public void deleteAddress(AppUser currentUser, Long addressId) {
        Address address = addressRepository
                .findByUserIdAndId(currentUser.getId(), addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Apply all mutable address fields from a request onto an Address entity.
     * Used for both create and update to avoid duplication.
     */
    private void applyAddressFields(Address address, AddressRequest request) {
        address.setRecipientName(request.getRecipientName());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setStateProvince(request.getStateProvince());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhone(request.getPhone());
        address.setDefault(request.isDefault());
    }
}
