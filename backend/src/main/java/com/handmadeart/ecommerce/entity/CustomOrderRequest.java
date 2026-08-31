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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code custom_order_request} table.
 *
 * Represents a customer's commissioned-artwork request and its full lifecycle
 * status (SRS §8.2, FR-CUST-01..11, Database Design &amp; ERD §3.13, §12).
 *
 * Key design decisions (ERD §12, §13.2):
 * <ul>
 *   <li>Thirteen lifecycle statuses (ERD §15.3) are enforced by a DB CHECK
 *       constraint; state-transition sequencing is enforced by the service layer.</li>
 *   <li>A request has at most one {@link Quotation} (UNIQUE FK on quotation side,
 *       ERD §13.2 / DEC-004 DEFERRED).</li>
 *   <li>{@code reviewedBy} is nullable — set when Admin takes action (FR-CUST-05).</li>
 *   <li>Optional descriptive fields ({@code designTheme}, {@code preferredColors},
 *       {@code dimensionsSize}, {@code budgetRange}, {@code requiredDeliveryDate},
 *       {@code additionalInstructions}) match FR-CUST-02 exactly.</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.13, §12, §15.3.
 */
@Entity
@Table(name = "custom_order_request")
public class CustomOrderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Requesting customer — FK to {@code app_user.id}, NOT NULL.
     * ON DELETE RESTRICT: a user with custom requests cannot be deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * Type of artwork being requested (FR-CUST-02).
     * Free-text VARCHAR(100) — not a controlled enum.
     */
    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;

    /**
     * Customer's description of the desired artwork (FR-CUST-02). NOT NULL.
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Optional design theme (FR-CUST-02).
     */
    @Column(name = "design_theme", length = 200)
    private String designTheme;

    /**
     * Optional preferred colors (FR-CUST-02).
     */
    @Column(name = "preferred_colors", length = 200)
    private String preferredColors;

    /**
     * Optional artwork dimensions/size specification (FR-CUST-02).
     */
    @Column(name = "dimensions_size", length = 100)
    private String dimensionsSize;

    /**
     * Optional customer budget range (FR-CUST-02).
     */
    @Column(name = "budget_range", length = 100)
    private String budgetRange;

    /**
     * Optional customer-requested delivery date (FR-CUST-02).
     */
    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;

    /**
     * Optional additional instructions from the customer (FR-CUST-02).
     */
    @Column(name = "additional_instructions", columnDefinition = "TEXT")
    private String additionalInstructions;

    /**
     * Request lifecycle status (SRS §8.2, ERD §15.3). DEFAULT 'REQUESTED'.
     * Thirteen approved values — CHECK constraint in migration.
     * Transition sequencing is enforced by the service layer (ERD §12.3).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomOrderRequestStatus status;

    /**
     * Admin who last reviewed/actioned the request (FR-CUST-05).
     * Nullable FK to {@code app_user.id}. ON DELETE RESTRICT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = true)
    private AppUser reviewedBy;

    /**
     * Admin's review or clarification notes (FR-CUST-06). Nullable.
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

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

    /**
     * Reference images for this request — LAZY.
     * ON DELETE CASCADE on the DB side (ERD §16): deleting a request removes its images.
     * Managed via {@link com.handmadeart.ecommerce.repository.CustomOrderImageRepository}.
     */
    @OneToMany(mappedBy = "customOrderRequest", fetch = FetchType.LAZY)
    private List<CustomOrderImage> images = new ArrayList<>();

    /**
     * The single quotation for this request (at most one — UNIQUE FK, ERD §13.2).
     * Nullable until Admin issues a quotation. LAZY.
     * Managed via {@link com.handmadeart.ecommerce.repository.QuotationRepository}.
     */
    @OneToOne(mappedBy = "customOrderRequest", fetch = FetchType.LAZY)
    private Quotation quotation;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CustomOrderRequest() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDesignTheme() { return designTheme; }
    public void setDesignTheme(String designTheme) { this.designTheme = designTheme; }

    public String getPreferredColors() { return preferredColors; }
    public void setPreferredColors(String preferredColors) { this.preferredColors = preferredColors; }

    public String getDimensionsSize() { return dimensionsSize; }
    public void setDimensionsSize(String dimensionsSize) { this.dimensionsSize = dimensionsSize; }

    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String budgetRange) { this.budgetRange = budgetRange; }

    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) {
        this.requiredDeliveryDate = requiredDeliveryDate;
    }

    public String getAdditionalInstructions() { return additionalInstructions; }
    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }

    public CustomOrderRequestStatus getStatus() { return status; }
    public void setStatus(CustomOrderRequestStatus status) { this.status = status; }

    public AppUser getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(AppUser reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<CustomOrderImage> getImages() { return images; }
    public Quotation getQuotation() { return quotation; }
}
