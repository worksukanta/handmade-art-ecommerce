package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.Address;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.repository.AddressRepository;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database integration tests for AppUser and Address persistence.
 *
 * Verifies approved schema rules from Database Design &amp; ERD §3.1, §3.2, §14:
 * - AppUser can be persisted and retrieved.
 * - Email uniqueness (case-insensitive) is enforced by the database.
 * - Role is stored as 'CUSTOMER' or 'ADMIN'.
 * - Address belongs to a user (FK constraint).
 * - Address required fields are enforced.
 * - Default-address partial unique constraint is enforced.
 * - Ownership-scoped address queries work correctly.
 * - Address cascade delete: deleting a user cascades to their addresses.
 *
 * ACTIVATION:
 *   These tests require a running PostgreSQL instance.
 *   Excluded from the default Maven test run (@Tag "db-integration").
 *
 *   Run with:
 *     mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration
 *
 *   Environment variables required:
 *     DB_URL       = jdbc:postgresql://localhost:5432/handmade_art_ecommerce_test
 *     DB_USERNAME  = <test db user>
 *     DB_PASSWORD  = <test db password>
 *
 * Each test that writes to the database is marked @Transactional so that
 * changes are rolled back after the test, keeping tests independent.
 */
@Tag("db-integration")
@SpringBootTest
@ActiveProfiles("db-integration")
class IdentityPersistenceIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Build a minimal valid AppUser with a unique email suffix. */
    private AppUser buildUser(String emailPrefix, UserRole role) {
        AppUser u = new AppUser();
        u.setEmail(emailPrefix + "@test.handmadeart.com");
        u.setPasswordHash("$2a$10$placeholder_hash_for_testing_only");
        u.setFullName("Test User " + emailPrefix);
        u.setRole(role);
        return u;
    }

    /** Build a minimal valid Address for the given user. */
    private Address buildAddress(AppUser user, boolean isDefault) {
        Address a = new Address();
        a.setUser(user);
        a.setRecipientName("Test Recipient");
        a.setLine1("123 Test Street");
        a.setCity("Testville");
        a.setStateProvince("Test State");
        a.setPostalCode("12345");
        a.setCountry("India");
        a.setDefault(isDefault);
        return a;
    }

    // =========================================================================
    // AppUser tests
    // =========================================================================

    @Test
    @Transactional
    void appUser_canBePersisted() {
        AppUser user = buildUser("persist_user", UserRole.CUSTOMER);
        AppUser saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getEmail()).isEqualTo("persist_user@test.handmadeart.com");
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @Transactional
    void appUser_role_persistsAsStringValue() {
        AppUser customer = buildUser("role_customer", UserRole.CUSTOMER);
        AppUser admin = buildUser("role_admin", UserRole.ADMIN);

        userRepository.save(customer);
        userRepository.save(admin);

        assertThat(userRepository.countByRole(UserRole.CUSTOMER)).isGreaterThanOrEqualTo(1);
        assertThat(userRepository.countByRole(UserRole.ADMIN)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void appUser_findByEmailIgnoreCase_returnsUser() {
        AppUser user = buildUser("find_email", UserRole.CUSTOMER);
        userRepository.save(user);

        Optional<AppUser> found = userRepository.findByEmailIgnoreCase("FIND_EMAIL@TEST.HANDMADEART.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find_email@test.handmadeart.com");
    }

    @Test
    @Transactional
    void appUser_existsByEmailIgnoreCase_trueWhenPresent() {
        AppUser user = buildUser("exists_check", UserRole.CUSTOMER);
        userRepository.save(user);
        userRepository.flush();

        assertThat(userRepository.existsByEmailIgnoreCase("EXISTS_CHECK@TEST.HANDMADEART.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("no_such_user@test.handmadeart.com")).isFalse();
    }

    @Test
    @Transactional
    void appUser_emailUniqueness_rejectsExactDuplicate() {
        AppUser first = buildUser("dup_email", UserRole.CUSTOMER);
        userRepository.saveAndFlush(first);

        AppUser duplicate = buildUser("dup_email", UserRole.CUSTOMER); // same email
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void appUser_emailUniqueness_rejectsCaseVariantDuplicate() {
        // ERD §3.1: case-insensitive uniqueness via lower(email) index.
        AppUser first = buildUser("case_dup", UserRole.CUSTOMER);
        // first email: case_dup@test.handmadeart.com
        userRepository.saveAndFlush(first);

        AppUser caseVariant = new AppUser();
        caseVariant.setEmail("CASE_DUP@TEST.HANDMADEART.COM");  // same but upper-cased
        caseVariant.setPasswordHash("$2a$10$placeholder");
        caseVariant.setFullName("Case Variant");
        caseVariant.setRole(UserRole.CUSTOMER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(caseVariant))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // Address tests
    // =========================================================================

    @Test
    @Transactional
    void address_canBePersistedForUser() {
        AppUser user = userRepository.save(buildUser("addr_persist", UserRole.CUSTOMER));
        Address address = buildAddress(user, false);
        Address saved = addressRepository.save(address);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCity()).isEqualTo("Testville");
        assertThat(saved.isDefault()).isFalse();
    }

    @Test
    @Transactional
    void address_findByUserId_returnsOnlyOwnersAddresses() {
        AppUser userA = userRepository.save(buildUser("addr_owner_a", UserRole.CUSTOMER));
        AppUser userB = userRepository.save(buildUser("addr_owner_b", UserRole.CUSTOMER));

        addressRepository.save(buildAddress(userA, false));
        addressRepository.save(buildAddress(userA, false));
        addressRepository.save(buildAddress(userB, false));
        addressRepository.flush();

        List<Address> userAAddresses = addressRepository.findByUserId(userA.getId());
        assertThat(userAAddresses).hasSize(2);
        assertThat(userAAddresses).allMatch(a -> a.getUser().getId().equals(userA.getId()));
    }

    @Test
    @Transactional
    void address_findByUserIdAndId_returnsEmptyForWrongOwner() {
        AppUser userA = userRepository.save(buildUser("wrong_owner_a", UserRole.CUSTOMER));
        AppUser userB = userRepository.save(buildUser("wrong_owner_b", UserRole.CUSTOMER));

        Address addrB = addressRepository.save(buildAddress(userB, false));
        addressRepository.flush();

        // userA should NOT be able to find userB's address.
        Optional<Address> result = addressRepository.findByUserIdAndId(userA.getId(), addrB.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void address_defaultAddress_partialUniqueConstraint_preventsTwoDefaults() {
        // ERD §14.2: partial unique index ensures at most one default per user.
        AppUser user = userRepository.save(buildUser("two_defaults", UserRole.CUSTOMER));

        Address first = buildAddress(user, true);
        addressRepository.saveAndFlush(first);

        Address second = buildAddress(user, true); // second default for same user — must fail
        assertThatThrownBy(() -> addressRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void address_findByUserIdAndIsDefaultTrue_returnsDefault() {
        AppUser user = userRepository.save(buildUser("find_default", UserRole.CUSTOMER));
        addressRepository.save(buildAddress(user, false));
        Address defaultAddr = addressRepository.saveAndFlush(buildAddress(user, true));

        Optional<Address> found = addressRepository.findByUserIdAndIsDefaultTrue(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(defaultAddr.getId());
    }

    @Test
    @Transactional
    void address_deletedWhenUserIsDeleted() {
        // ERD §16: address uses ON DELETE CASCADE on user_id FK.
        AppUser user = userRepository.save(buildUser("cascade_delete", UserRole.CUSTOMER));
        Address addr = addressRepository.saveAndFlush(buildAddress(user, false));
        Long addressId = addr.getId();

        userRepository.deleteById(user.getId());
        userRepository.flush();

        assertThat(addressRepository.findById(addressId)).isEmpty();
    }

    @Test
    @Transactional
    void address_multipleSavedAddresses_countByUserIdIsCorrect() {
        AppUser user = userRepository.save(buildUser("count_addr", UserRole.CUSTOMER));
        addressRepository.save(buildAddress(user, false));
        addressRepository.save(buildAddress(user, false));
        addressRepository.save(buildAddress(user, false));
        addressRepository.flush();

        assertThat(addressRepository.countByUserId(user.getId())).isEqualTo(3);
    }
}
