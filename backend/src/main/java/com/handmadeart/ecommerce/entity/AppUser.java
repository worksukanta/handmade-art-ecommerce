package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code app_user} table.
 *
 * Represents a registered platform user — either a CUSTOMER or an ADMIN.
 *
 * Approved schema source: Database Design &amp; ERD §3.1.
 *
 * Design notes:
 * <ul>
 *   <li>Primary key: BIGINT GENERATED ALWAYS AS IDENTITY → GenerationType.IDENTITY.</li>
 *   <li>email: unique via a case-insensitive index on {@code lower(email)} defined in the
 *       Flyway migration; the {@code unique = true} attribute on the column additionally
 *       signals the constraint to Hibernate for schema-validation purposes.</li>
 *   <li>password_hash: stores only a BCrypt hash — never plaintext.  Hashing is
 *       implemented in Phase 3 (Authentication).</li>
 *   <li>role: persisted as a VARCHAR(10) string matching the CHECK constraint values
 *       'CUSTOMER'/'ADMIN'.  EnumType.STRING guarantees stored values match enum names
 *       regardless of declaration order.</li>
 *   <li>addresses: LAZY one-to-many; not fetched unless explicitly needed, avoiding
 *       accidental N+1 loads.  CascadeType is intentionally omitted — address lifecycle
 *       is managed independently through AddressRepository.</li>
 *   <li>created_at / updated_at: set by the database DEFAULT now(); the application
 *       does not override them on insert, ensuring the DB clock is authoritative.</li>
 * </ul>
 *
 * No controller or service uses this entity directly in Phase 2B.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Login identifier.  Must be unique; case-insensitive uniqueness is enforced by
     * a partial unique index on {@code lower(email)} in the Flyway migration.
     */
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    /**
     * BCrypt hash of the user's password (FR-AUTH-04).
     * The application MUST NEVER store a plaintext password here.
     * Hashing behaviour is implemented in Phase 3.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Fixed application role.  Persisted as the enum name string so that
     * 'CUSTOMER' and 'ADMIN' are stored directly in the CHECK-constrained column.
     * Default value is handled by the database column default; the application
     * must always supply an explicit role on registration.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role;

    // DB DEFAULT now() is authoritative for creation time.
    // insertable = false: Hibernate omits this column from INSERT so the DB DEFAULT fires.
    // @Generated(INSERT): instructs Hibernate to re-SELECT this column after INSERT
    // so the Java field is populated with the DB-assigned value.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    // DB DEFAULT now() seeds updated_at on INSERT.
    // Application code must set this field before UPDATE operations (Phase 3+).
    // @Generated(INSERT, UPDATE): Hibernate re-SELECTs after both INSERT and UPDATE
    // so the Java field reflects whatever value PostgreSQL holds.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    /**
     * The customer's delivery addresses.
     * Fetched LAZILY — only loaded when explicitly accessed.
     * Cascade is intentionally absent; addresses are managed via AddressRepository.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Address> addresses = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AppUser() {
        // JPA requires a no-arg constructor.
        // Public so that application code and tests can instantiate this entity.
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Address> getAddresses() {
        return addresses;
    }
}
