package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Quotation}.
 *
 * Approved operations: FR-CUST-07..10, UC-014, UC-015.
 *
 * Composite index on {@code quotation(status, expiry_at)} supports efficiently
 * finding pending quotations whose {@code expiry_at} has passed (BR-06, FR-CUST-10,
 * ERD §17).
 */
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    /**
     * Find the quotation for a given custom order request.
     * One-to-one relationship — at most one quotation per request (ERD §13.2).
     * Used for displaying quotation details to the customer.
     */
    Optional<Quotation> findByCustomOrderRequestId(Long customOrderRequestId);

    /**
     * Find all quotations with a given status.
     * Supports Admin operations such as finding all pending or expired quotations
     * (composite index on status + expiry_at serves this query, ERD §17).
     */
    List<Quotation> findByStatus(QuotationStatus status);
}
