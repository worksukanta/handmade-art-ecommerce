package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code address} table.
 *
 * Represents a customer delivery address owned by one {@link AppUser}.
 *
 * Approved schema source: Database Design &amp; ERD §3.2 and §14.
 *
 * Design notes:
 * <ul>
 *   <li>Ownership: {@code user_id} FK → {@code app_user.id}, NOT NULL.  A customer may
 *       have zero-to-many addresses; each address belongs to exactly one customer.</li>
 *   <li>is_default: BOOLEAN NOT NULL DEFAULT false.  "Only one default per customer" is
 *       enforced by a partial unique index on {@code address(user_id) WHERE is_default = true}
 *       defined in the Flyway migration (ERD §14.2).  This entity does not enforce that
 *       constraint in Java — it belongs at the database layer.</li>
 *   <li>Fetch strategy for user: LAZY.  Loading an address should not automatically load
 *       the full AppUser graph.</li>
 *   <li>The address snapshot columns on {@code customer_order} (ship_*) are intentionally
 *       separate — orders copy address fields at checkout time so that later edits or
 *       deletions of this entity do not affect historical orders (ERD §9.3 / §14.4).</li>
 *   <li>created_at / updated_at: set by DB DEFAULT now(); not overridden by the application
 *       on insert.</li>
 * </ul>
 *
 * No controller or service uses this entity directly in Phase 2B.
 */
@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning customer — FK to {@code app_user.id}, NOT NULL.
     * Fetched LAZILY to avoid loading the full user when only address data is needed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "line2", length = 255)
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state_province", nullable = false, length = 100)
    private String stateProvince;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Marks the customer's preferred/default address for checkout pre-selection.
     * "At most one default per customer" is enforced by a PostgreSQL partial unique index
     * in the migration:  UNIQUE ON address(user_id) WHERE is_default = true.
     * Default: false (set by DB column default).
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

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

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Address() {
        // JPA requires a no-arg constructor.
        // Public so that application code and tests can instantiate this entity.
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
