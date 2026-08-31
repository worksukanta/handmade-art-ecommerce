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
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code customer_order} table.
 *
 * Records a confirmed ready-made purchase. Uses table name {@code customer_order}
 * because {@code order} is a reserved SQL keyword.
 *
 * Key design decisions (Database Design &amp; ERD §9):
 * <ul>
 *   <li>Shipping address fields are a snapshot copied at checkout time, not a FK to
 *       {@link Address}. This preserves historical accuracy if the customer later edits
 *       or deletes the address (ERD §9.3, §6.1).</li>
 *   <li>{@code subtotal_amount} and {@code total_amount} are server-computed and stored;
 *       tax/delivery columns are intentionally absent (DEC-007 DEFERRED, ERD §9.4).</li>
 *   <li>Status uses the six-value lifecycle defined in SRS §8.1 (ERD §15.1).</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.9, §9.
 */
@Entity
@Table(name = "customer_order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Purchasing customer — FK to {@code app_user.id}, NOT NULL.
     * ON DELETE RESTRICT: a user with orders cannot be deleted (ERD §16).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * Order lifecycle status (SRS §8.1). DEFAULT 'PENDING_PAYMENT'.
     * Persisted as the enum name string matching the CHECK constraint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    // -------------------------------------------------------------------------
    // Shipping address snapshot (ERD §9.3) — copied from Address at checkout;
    // never a FK so edits/deletions of Address do not affect historical orders.
    // -------------------------------------------------------------------------

    @Column(name = "ship_recipient_name", nullable = false, length = 150)
    private String shipRecipientName;

    @Column(name = "ship_line1", nullable = false, length = 255)
    private String shipLine1;

    @Column(name = "ship_line2", length = 255)
    private String shipLine2;

    @Column(name = "ship_city", nullable = false, length = 100)
    private String shipCity;

    @Column(name = "ship_state_province", nullable = false, length = 100)
    private String shipStateProvince;

    @Column(name = "ship_postal_code", nullable = false, length = 20)
    private String shipPostalCode;

    @Column(name = "ship_country", nullable = false, length = 100)
    private String shipCountry;

    @Column(name = "ship_phone", length = 20)
    private String shipPhone;

    // -------------------------------------------------------------------------
    // Totals — server-authoritative (FR-CART-04/07, BR-10, ERD §9.4).
    // Tax and delivery columns are intentionally absent (DEC-007 DEFERRED).
    // -------------------------------------------------------------------------

    /** Sum of order-item line totals. CHECK >= 0. */
    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /** Server-authoritative total. CHECK >= 0. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // DB DEFAULT now() is authoritative. insertable = false omits from INSERT.
    // @Generated(INSERT): Hibernate re-SELECTs after INSERT to populate the field.
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    // DB DEFAULT now() seeds updated_at on INSERT.
    // @Generated(INSERT, UPDATE): Hibernate re-SELECTs after INSERT and UPDATE.
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    /** Order line items — LAZY. Cascade absent; managed via OrderItemRepository. */
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /** Payment attempts for this order — LAZY. Managed via PaymentRepository. */
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CustomerOrder() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getShipRecipientName() { return shipRecipientName; }
    public void setShipRecipientName(String shipRecipientName) { this.shipRecipientName = shipRecipientName; }

    public String getShipLine1() { return shipLine1; }
    public void setShipLine1(String shipLine1) { this.shipLine1 = shipLine1; }

    public String getShipLine2() { return shipLine2; }
    public void setShipLine2(String shipLine2) { this.shipLine2 = shipLine2; }

    public String getShipCity() { return shipCity; }
    public void setShipCity(String shipCity) { this.shipCity = shipCity; }

    public String getShipStateProvince() { return shipStateProvince; }
    public void setShipStateProvince(String shipStateProvince) { this.shipStateProvince = shipStateProvince; }

    public String getShipPostalCode() { return shipPostalCode; }
    public void setShipPostalCode(String shipPostalCode) { this.shipPostalCode = shipPostalCode; }

    public String getShipCountry() { return shipCountry; }
    public void setShipCountry(String shipCountry) { this.shipCountry = shipCountry; }

    public String getShipPhone() { return shipPhone; }
    public void setShipPhone(String shipPhone) { this.shipPhone = shipPhone; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public List<Payment> getPayments() { return payments; }
}
