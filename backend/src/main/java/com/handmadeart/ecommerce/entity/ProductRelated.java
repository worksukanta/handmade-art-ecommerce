package com.handmadeart.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code product_related} junction table.
 *
 * Records a curated directional relationship between two products.
 * Used to present related/similar products on a product-detail page (FR-CAT-07).
 *
 * Approved schema source: Database Design &amp; ERD §3.6, §7.4.
 *
 * Design notes:
 * <ul>
 *   <li>Composite PK: (product_id, related_product_id) — prevents duplicate pairs.</li>
 *   <li>CHECK product_id &lt;&gt; related_product_id: a product cannot be related to itself
 *       (ERD §3.6 row constraint).  Enforced in the migration SQL.</li>
 *   <li>Directionality: the relationship is curated directionally — Product A → Product B
 *       does not automatically imply Product B → Product A.  The application layer may
 *       query symmetrically if desired (ERD §7.4), but no symmetric constraint is in
 *       the schema.</li>
 *   <li>Both FKs use ON DELETE CASCADE (ERD §16): removing a product removes all
 *       product_related rows referencing it on either side.</li>
 * </ul>
 */
@Entity
@Table(name = "product_related")
public class ProductRelated {

    @EmbeddedId
    private ProductRelatedId id;

    /**
     * The product being viewed — FK to product.id, part 1 of composite PK.
     */
    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The related product to present — FK to product.id, part 2 of composite PK.
     */
    @MapsId("relatedProductId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_product_id", nullable = false)
    private Product relatedProduct;

    // DB DEFAULT now() is authoritative for creation time.
    // insertable = false: Hibernate omits this column from INSERT so the DB DEFAULT fires.
    // @Generated(INSERT): instructs Hibernate to re-SELECT this column after INSERT
    // so the Java field is populated with the DB-assigned value.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProductRelated() {
    }

    public ProductRelated(Product product, Product relatedProduct) {
        this.id = new ProductRelatedId(product.getId(), relatedProduct.getId());
        this.product = product;
        this.relatedProduct = relatedProduct;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public ProductRelatedId getId() { return id; }
    public void setId(ProductRelatedId id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Product getRelatedProduct() { return relatedProduct; }
    public void setRelatedProduct(Product relatedProduct) { this.relatedProduct = relatedProduct; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
