package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.admin.AdminCustomerResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for admin customer management (read-only).
 *
 * REST API Spec §16:
 *   GET /api/v1/admin/customers       — paginated customer list
 *   GET /api/v1/admin/customers/{id}  — customer detail
 *
 * Read-only. No customer mutation, deletion, or role management.
 * Password hash is never exposed in any response.
 *
 * The approved specification targets customers (CUSTOMER role). Only CUSTOMER
 * accounts are returned in the list so admin accounts are not surfaced.
 */
@Service
@Transactional(readOnly = true)
public class AdminCustomerService {

    private final AppUserRepository userRepository;

    public AdminCustomerService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================================================================
    // GET /api/v1/admin/customers — paginated customer list
    // =========================================================================

    /**
     * Return all CUSTOMER accounts, paginated.
     * ADMIN accounts are excluded (approved contract: customer list).
     *
     * Default sort: most recently created first.
     *
     * @param page zero-based page index
     * @param size page size (default 20)
     * @return paginated customer summaries
     */
    public PageResponse<AdminCustomerResponse> listCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AppUser> userPage = userRepository.findByRole(UserRole.CUSTOMER, pageable);
        Page<AdminCustomerResponse> dtoPage = userPage.map(AdminCustomerResponse::from);
        return PageResponse.from(dtoPage);
    }

    // =========================================================================
    // GET /api/v1/admin/customers/{id} — customer detail
    // =========================================================================

    /**
     * Return a single customer by id.
     *
     * Only CUSTOMER-role accounts are accessible; an admin account id returns 404
     * (same non-disclosure semantics — admin accounts are not surfaced to admin-customer API).
     *
     * @param customerId path variable
     * @return AdminCustomerResponse
     */
    public AdminCustomerResponse getCustomer(Long customerId) {
        AppUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found");
        }
        return AdminCustomerResponse.from(user);
    }
}
