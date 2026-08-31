package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Address}.
 *
 * Only methods required by clearly-approved workflows are declared here.
 *
 * Approved method justification:
 * <ul>
 *   <li>{@code findByUserId} — required by Address management (Phase 2B / later)
 *       to list all addresses for a given customer (UC-003, FR-AUTH-07).</li>
 *   <li>{@code findByUserIdAndId} — used by Address update/delete to verify
 *       ownership before modifying a specific address (BR-06, BR-09).</li>
 *   <li>{@code findByUserIdAndIsDefaultTrue} — used at checkout pre-selection
 *       to find the customer's default address (DEC-010 deferred for service
 *       behavior; the query is needed regardless).</li>
 *   <li>{@code countByUserId} — useful for limiting or listing address counts.</li>
 * </ul>
 *
 * Ownership enforcement (a customer cannot see another customer's addresses)
 * is the responsibility of the service layer in Phase 3+ — the repository
 * methods are scoped by user_id to support that enforcement, but the service
 * must pass the authenticated user's own id, never a client-supplied value.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Return all addresses belonging to the specified customer.
     * Results are unordered; the service layer or caller may apply ordering.
     */
    List<Address> findByUserId(Long userId);

    /**
     * Return a specific address only if it belongs to the specified customer.
     * Used for ownership-verified single-address access (BR-06/BR-09).
     */
    Optional<Address> findByUserIdAndId(Long userId, Long addressId);

    /**
     * Return the customer's default address, if one is set.
     * The partial unique index guarantees at most one row matches.
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Count the addresses belonging to a customer.
     */
    long countByUserId(Long userId);
}
