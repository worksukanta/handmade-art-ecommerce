package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser}.
 *
 * Only methods required by clearly-approved workflows are declared here.
 * Speculative query methods are not added.
 *
 * Approved method justification:
 * <ul>
 *   <li>{@code findByEmailIgnoreCase} — required by Authentication (Phase 3)
 *       for credential lookup on login (FR-AUTH-03).  Declared now because the
 *       approved schema explicitly defines case-insensitive email uniqueness and
 *       the authentication workflow is unambiguously approved.</li>
 *   <li>{@code existsByEmailIgnoreCase} — required by registration (Phase 3)
 *       for duplicate-email checking (FR-AUTH-02).
 *   <li>{@code countByRole} — useful for Admin user listing (UC-019/020).</li>
 * </ul>
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Find a user by email address using case-insensitive comparison.
     * Used by the authentication flow (Phase 3) to locate the account
     * matching the submitted login credential (FR-AUTH-03).
     *
     * The underlying unique index on {@code lower(email)} makes this query
     * efficient and consistent with the approved uniqueness semantics.
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Check whether an email address is already registered.
     * Used by the registration flow (Phase 3) to enforce FR-AUTH-02
     * (duplicate email rejection) without loading the full entity.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Count users by role.
     * Supports admin user-listing queries (UC-019/020).
     */
    long countByRole(UserRole role);

    /**
     * Return a page of users by role — used by admin customer list (UC-019).
     * CUSTOMER accounts only are returned; ADMIN accounts excluded.
     */
    org.springframework.data.domain.Page<AppUser> findByRole(
            UserRole role, org.springframework.data.domain.Pageable pageable);

    /**
     * Find a user by email using a JPQL query that explicitly lower-cases both
     * sides — an alternative for callers that already have a lower-cased value.
     * The standard {@code findByEmailIgnoreCase} is preferred in most cases.
     */
    @Query("SELECT u FROM AppUser u WHERE lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailLowerCase(String email);
}
