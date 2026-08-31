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

import java.math.BigDecimal;

/**
 * JPA entity for the {@code order_item} table.
 *
 * One line item in a confirmed order. Stores purchase-time snapshots of the
 * product name and unit price so historical order records remain accurate
 * regardless of later product changes (FR-ORD-03, BR-11, ERD §9.2).
 *
 * Key design notes:
 * <ul>
 *   <li>{@code product_id}: nullable FK → {@code product.id}, ON DELETE SET NULL.
 *       The FK is retained for traceability but is NOT the source of truth for what
 *       was charged — the snapshot fields are (ERD §9.2).</li>
 *   <li>{@code product_name_snapshot} and {@code unit_price_snapshot} are captured at
 *       order-creation time and are never overwritten.</li>
 *   <li>{@code line_total} = unit_price_snapshot × quantity, stored at insert for
 *       efficient reporting; never recalculated from live data.</li>
 *   <li>No timestamps defined in the approved schema for OrderItem (ERD §3.10).</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.10, §9.
 */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning order — FK to {@code customer_order.id}, NOT NULL.
     * ON DELETE CASCADE: deleting an order removes all its items (ERD §16).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    /**
     * Reference to the source product — nullable, ON DELETE SET NULL.
     * Kept for traceability/navigation; not the source of truth for price/name
     * (ERD §9.2). The Java field is nullable to allow historical rows to
     * survive a future product hard-delete.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    /**
     * Purchase-time product name (BR-11). NOT NULL.
     * Preserved independently of any later rename on the Product.
     */
    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot;

    /**
     * Purchase-time unit price (FR-ORD-03, BR-11). NOT NULL, CHECK >= 0.
     * BigDecimal preserves the NUMERIC(10,2) precision.
     */
    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    /** Purchased quantity. CHECK quantity > 0 enforced in the DB (ERD §3.10). */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Line total = unit_price_snapshot × quantity. CHECK >= 0.
     * Stored at insert time; never recalculated from live data.
     */
    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public OrderItem() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
