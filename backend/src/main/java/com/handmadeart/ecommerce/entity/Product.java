package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code product} table.
 *
 * Represents a ready-made, custom-available, or portfolio-only artwork item.
 *
 * Approved schema source: Database Design &amp; ERD §3.4, §7.
 *
 * Design notes:
 * <ul>
 *   <li>price: NUMERIC(10,2) mapped as {@link BigDecimal} — never float/double for
 *       monetary values (ERD §3, BR-15).</li>
 *   <li>product_type: CHECK-constrained VARCHAR mapped as {@link ProductType} enum.</li>
 *   <li>status: CHECK-constrained VARCHAR mapped as {@link ProductStatus} enum.</li>
 *   <li>category: LAZY ManyToOne — FK to category.id, NOT NULL.  On DELETE RESTRICT
 *       (ERD §16) so a category cannot be deleted while products exist.</li>
 *   <li>images: LAZY OneToMany — cascade intentionally absent; image lifecycle managed
 *       via ProductImageRepository.  ON DELETE CASCADE on the FK side (ERD §16).</li>
 *   <li>inventory: LAZY OneToOne — product_id is both PK and FK on the inventory side.</li>
 *   <li>No SKU or external identifier column is defined in the approved ERD.</li>
 * </ul>
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning category — FK to {@code category.id}, NOT NULL.
     * A product is assigned to exactly one category (FR-PROD-05).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Product price — NUMERIC(10,2), CHECK >= 0 (FR-PROD-07, BR-15).
     * BigDecimal preserves exact decimal precision.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Product type — READY_MADE, CUSTOM_AVAILABLE, or PORTFOLIO_ONLY (FR-PROD-06).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    /**
     * Visibility status — ACTIVE (customer-visible) or INACTIVE (FR-PROD-03/08).
     * Database default: 'ACTIVE'.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ProductStatus status;

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

    /** Product images — LAZY.  Cascade absent; managed via ProductImageRepository. */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    /**
     * Inventory record — LAZY OneToOne (product_id is both PK and FK on inventory side).
     * Only READY_MADE and CUSTOM_AVAILABLE products have an inventory row.
     */
    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private Inventory inventory;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Product() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<ProductImage> getImages() { return images; }

    public Inventory getInventory() { return inventory; }
}
