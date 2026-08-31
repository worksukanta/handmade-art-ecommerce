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
 * JPA entity for the {@code category} table.
 *
 * Represents a catalogue classification used to organise products.
 *
 * Approved schema source: Database Design &amp; ERD §3.3, §7.
 *
 * Design notes:
 * <ul>
 *   <li>No parent-category hierarchy column is defined in the approved schema.</li>
 *   <li>status: CHECK-constrained VARCHAR mapped as {@link CategoryStatus} enum string.</li>
 *   <li>products collection: LAZY; cascade is intentionally absent — product lifecycle
 *       is managed through ProductRepository independently.</li>
 *   <li>FK on product.category_id uses ON DELETE RESTRICT (ERD §16), so a category with
 *       products cannot be hard-deleted — deactivation via status is the approved path.</li>
 * </ul>
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Category name — must be unique across all categories (ERD §3.3). */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Visibility status.  Persisted as the enum name string ('ACTIVE' / 'INACTIVE').
     * Database default: 'ACTIVE'.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CategoryStatus status;

    // DB DEFAULT now() is authoritative for creation time.
    // insertable = false: Hibernate omits this column from INSERT so the DB DEFAULT fires.
    // @Generated(INSERT): instructs Hibernate to re-SELECT this column after INSERT
    // so the Java field is populated with the DB-assigned value.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    /** Products in this category.  LAZY — not auto-loaded. */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Category() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CategoryStatus getStatus() { return status; }
    public void setStatus(CategoryStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public List<Product> getProducts() { return products; }
}
