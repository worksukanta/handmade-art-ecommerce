package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code inventory} table.
 *
 * Tracks available stock quantity for a ready-made or custom-available product.
 * This is a 1:1 extension of Product: {@code product_id} is both the primary key
 * and the foreign key back to {@code product.id} (ERD §3.12, §11).
 *
 * Design notes:
 * <ul>
 *   <li>PORTFOLIO_ONLY products do not have an inventory row (ERD §11.1).</li>
 *   <li>quantity_on_hand: CHECK >= 0 enforced in migration SQL (FR-INV-01, BR-15).
 *       The database guarantees stock never goes negative as a final defence.</li>
 *   <li>No version/locking column is added here — DEC-009 (inventory concurrency
 *       strategy) remains OPEN.  The concurrency mechanism (optimistic version column,
 *       SELECT FOR UPDATE, etc.) will be added when DEC-009 is resolved, in the
 *       appropriate transactional implementation phase.</li>
 *   <li>updated_at: set by DB DEFAULT now(); should be updated by the application
 *       on every stock change.</li>
 * </ul>
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    /**
     * product_id is both PK and FK → product.id.
     * @Id + @MapsId + @OneToOne produces a shared-primary-key mapping.
     */
    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * The product this inventory row belongs to.
     * {@code @MapsId} links the entity's own {@code productId} field to the
     * associated Product's id.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Current stock level.  CHECK >= 0 in the DB (FR-INV-01, BR-15).
     * Default: 0.
     */
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    // DB DEFAULT now() seeds updated_at on INSERT.
    // Application code must set this field before stock-change operations (Phase 3+).
    // @Generated(INSERT, UPDATE): Hibernate re-SELECTs after both INSERT and UPDATE
    // so the Java field reflects whatever value PostgreSQL holds.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Inventory() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getProductId() { return productId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
