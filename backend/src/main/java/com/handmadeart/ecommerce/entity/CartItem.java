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
 * JPA entity for the {@code cart_item} table.
 *
 * One line item in a customer's cart — a product with a quantity.
 * A product appears at most once per cart; quantity is updated in place
 * (UNIQUE (cart_id, product_id) enforced in the migration, ERD §3.8).
 *
 * CartItem intentionally does NOT store price: a cart must always reflect
 * the live product price and availability — freezing occurs only at order creation
 * (Database Design &amp; ERD §8.3, §6.1).
 *
 * Approved schema source: Database Design &amp; ERD §3.8, §8.
 */
@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning cart — FK to {@code cart.id}, NOT NULL.
     * ON DELETE CASCADE: removing a cart removes all its items (ERD §16).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * Selected product — FK to {@code product.id}, NOT NULL.
     * ON DELETE RESTRICT: a product in a cart cannot be hard-deleted without
     * removing or clearing the cart item first (ERD §16).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Desired quantity. CHECK quantity > 0 enforced in the DB (FR-CART-02, ERD §3.8).
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // DB DEFAULT now() is authoritative. insertable = false omits from INSERT.
    // @Generated(INSERT): Hibernate re-SELECTs after INSERT to populate the field.
    @Generated(event = EventType.INSERT)
    @Column(name = "added_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime addedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CartItem() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public OffsetDateTime getAddedAt() { return addedAt; }
}
