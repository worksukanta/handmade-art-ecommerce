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
 * JPA entity for the {@code custom_order_image} table.
 *
 * Stores reference-image file metadata for a {@link CustomOrderRequest}.
 * The binary image file is stored on the filesystem — only metadata is persisted
 * here (SDD §13, FR-CUST-03, BR-14, Database Design &amp; ERD §3.14).
 *
 * Design notes:
 * <ul>
 *   <li>{@code customOrderRequest}: LAZY ManyToOne FK → custom_order_request.id, NOT NULL.
 *       ON DELETE CASCADE on the DB side (ERD §16): deleting a request removes its images.</li>
 *   <li>{@code storageReference}: server-generated path/key — never the original client
 *       filename (SDD §13.3 path-traversal prevention).</li>
 *   <li>{@code fileSizeBytes}: CHECK &gt; 0 enforced in SQL (BR-14).</li>
 *   <li>No {@code displayOrder} or {@code isPrimary} — reference images are not a gallery;
 *       those fields are specific to the product-image catalogue use-case (ERD §3.14 vs §3.5).</li>
 * </ul>
 *
 * Approved schema source: Database Design &amp; ERD §3.14, §12.1.
 */
@Entity
@Table(name = "custom_order_image")
public class CustomOrderImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Owning request — FK to {@code custom_order_request.id}, NOT NULL.
     * Deleted (CASCADE) when its parent request is deleted (ERD §16).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "custom_order_request_id", nullable = false)
    private CustomOrderRequest customOrderRequest;

    /**
     * Server-generated file storage path/key (SDD §13.3).
     * Never derived from the original client-supplied filename.
     */
    @Column(name = "storage_reference", nullable = false, length = 500)
    private String storageReference;

    /**
     * Original client filename stored for display only — never used as a storage path
     * (SDD §13.3 path-traversal prevention).
     */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /**
     * Validated MIME type (e.g., image/jpeg, image/png). BR-14.
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * Validated file size in bytes. CHECK &gt; 0 enforced in migration SQL (BR-14).
     */
    @Column(name = "file_size_bytes", nullable = false)
    private Integer fileSizeBytes;

    // DB DEFAULT now() is authoritative for upload time.
    // insertable = false: Hibernate omits this column from INSERT so the DB DEFAULT fires.
    // @Generated(INSERT): instructs Hibernate to re-SELECT this column after INSERT.
    @Generated(event = EventType.INSERT)
    @Column(name = "uploaded_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime uploadedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CustomOrderImage() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public CustomOrderRequest getCustomOrderRequest() { return customOrderRequest; }
    public void setCustomOrderRequest(CustomOrderRequest customOrderRequest) {
        this.customOrderRequest = customOrderRequest;
    }

    public String getStorageReference() { return storageReference; }
    public void setStorageReference(String storageReference) { this.storageReference = storageReference; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Integer fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
}
