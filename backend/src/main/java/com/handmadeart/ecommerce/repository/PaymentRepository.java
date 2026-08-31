package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Payment}.
 *
 * Approved operations: FR-PAY-01..06, UC-008, UC-015.
 *
 * Note: {@code customOrderRequestId} is stored as a raw Long in the Payment entity
 * until Phase 2E creates the {@code CustomOrderRequest} entity. Queries by
 * custom_order_request_id use the raw column value.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find all payment attempts for a given order (includes PENDING, SUCCESS, FAILED).
     * Index on payment.order_id serves this query (ERD §17).
     */
    List<Payment> findByOrderId(Long orderId);

    /**
     * Find all payment attempts for a given custom order request.
     * Index on payment.custom_order_request_id serves this query (ERD §17).
     */
    List<Payment> findByCustomOrderRequestId(Long customOrderRequestId);

    /**
     * Find the successful payment for a given order — used for order-history display.
     */
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    /**
     * Find a payment by the provider's transaction reference — used for
     * reconciliation and webhook/callback handling (FR-PAY-02).
     */
    Optional<Payment> findByProviderTransactionReference(String reference);
}
