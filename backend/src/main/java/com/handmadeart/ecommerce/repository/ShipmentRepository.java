package com.handmadeart.ecommerce.repository;

import com.handmadeart.ecommerce.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Shipment}.
 *
 * Approved operations: FR-SHIP-01..03, UC-018 (DEC-008 APPROVED: status/tracking only).
 */
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /**
     * Find the shipment for a given ready-made order.
     * An order has at most one shipment (ERD §5).
     */
    Optional<Shipment> findByOrderId(Long orderId);

    /**
     * Find the shipment for a given custom order request.
     * A completed custom order has at most one shipment (ERD §5).
     */
    Optional<Shipment> findByCustomOrderRequestId(Long customOrderRequestId);
}
