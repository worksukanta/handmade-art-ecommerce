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
 * JPA entity for the {@code product_image} table.
 *
 * Stores file metadata for a product's images.
 * The binary image file itself is stored on the filesystem — only metadata is persisted here
 * (System Design Document §13, Database Design &amp; ERD §3.5).
 *
 * Design notes:
 * <ul>
 *   <li>product: LAZY ManyToOne FK → product.id, NOT NULL.  ON DELETE CASCADE on the
 *       DB side (ERD §16): deleting a product removes all its image records.</li>
 *   <li>storage_reference: server-generated path/key — never the original client filename
 *       (SDD §13.3 path-traversal prevention).</li>
 *   <li>file_size_bytes: CHECK > 0 — enforced in SQL; the entity stores the validated value.</li>
 *   <li>display_order: DEFAULT 0; used to order multiple images in the product gallery.</li>
 *   <li>is_primary: marks the catalogue thumbnail.</li>
 * </ul>
 */
@Entity
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Owning product — FK to product.id, NOT NULL.  Deleted when product is deleted. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Server-generated file storage path/key (SDD §13.3).
     * Never derived from the original client-supplied filename.
     */
    @Column(name = "storage_reference", nullable = false, length = 500)
    private String storageReference;

    /** Original client filename stored for display only — never used as a storage path. */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** Validated MIME type (e.g., image/jpeg, image/png). */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** Validated file size in bytes.  CHECK > 0 enforced in migration SQL. */
    @Column(name = "file_size_bytes", nullable = false)
    private Integer fileSizeBytes;

    /** Display order for multi-image gallery.  Default 0. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** True if this image is the primary catalogue thumbnail for the product. */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    // DB DEFAULT now() is authoritative for upload time.
    // insertable = false: Hibernate omits this column from INSERT so the DB DEFAULT fires.
    // @Generated(INSERT): instructs Hibernate to re-SELECT this column after INSERT
    // so the Java field is populated with the DB-assigned value.
    @Generated(event = EventType.INSERT)
    @Column(name = "uploaded_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime uploadedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProductImage() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getStorageReference() { return storageReference; }
    public void setStorageReference(String storageReference) { this.storageReference = storageReference; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Integer fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
}
