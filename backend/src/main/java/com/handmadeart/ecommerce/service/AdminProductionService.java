package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentStatusUpdateRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.entity.ShipmentStatus;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Service for Admin production workflow and shipment management (Phase 3E.2 scope).
 *
 * Admin endpoints:
 *   PATCH /api/v1/admin/custom-requests/{id}/status — updateCustomRequestStatus
 *   POST  /api/v1/admin/shipments                   — createShipment
 *   PATCH /api/v1/admin/shipments/{id}/status        — updateShipmentStatus
 *   GET   /api/v1/admin/shipments/{id}               — adminGetShipment
 *
 * Admin production transitions (ERD §15.3 — approved workflow):
 *   IN_PRODUCTION → COMPLETED
 *   COMPLETED     → SHIPPED
 *   SHIPPED       → DELIVERED
 *
 * Only exact approved next states are permitted — no arbitrary jumps.
 * CUSTOMER → 403 on all admin endpoints (SecurityConfig route rule).
 *
 * DEC-008 APPROVED: MVP shipping is status/tracking based; no carrier API integration.
 */
@Service
public class AdminProductionService {

    private final CustomOrderRequestRepository requestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomOrderImageRepository imageRepository;
    private final ShipmentRepository shipmentRepository;

    public AdminProductionService(
            CustomOrderRequestRepository requestRepository,
            CustomerOrderRepository customerOrderRepository,
            CustomOrderImageRepository imageRepository,
            ShipmentRepository shipmentRepository) {
        this.requestRepository = requestRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.imageRepository = imageRepository;
        this.shipmentRepository = shipmentRepository;
    }

    // =========================================================================
    // PATCH /api/v1/admin/custom-requests/{id}/status — production state update
    // =========================================================================

    /**
     * Admin updates the production status of a custom artwork request.
     *
     * Approved admin-driven transitions (ERD §15.3):
     *   IN_PRODUCTION → COMPLETED
     *   COMPLETED     → SHIPPED
     *   SHIPPED       → DELIVERED
     *
     * Any other transition is rejected → 409 INVALID_TRANSITION.
     * CUSTOMER operations must not directly set arbitrary request statuses —
     * this endpoint is ADMIN-only (enforced by SecurityConfig /api/v1/admin/**).
     *
     * @param requestId  path variable — the custom request
     * @param newStatus  target status
     * @return updated CustomArtworkRequestResponse
     */
    @Transactional
    public CustomArtworkRequestResponse updateCustomRequestStatus(Long requestId,
                                                                   CustomOrderRequestStatus newStatus) {

        CustomOrderRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        validateAdminProductionTransition(req.getStatus(), newStatus);

        req.setStatus(newStatus);
        CustomOrderRequest saved = requestRepository.save(req);
        return buildResponse(saved);
    }

    // =========================================================================
    // POST /api/v1/admin/shipments — Admin creates a shipment record
    // =========================================================================

    /**
     * Admin creates a new shipment record for a custom request or ready-made order.
     *
     * Exactly one of customOrderRequestId or orderId must be provided.
     * The referenced entity must exist. A shipment record for the parent may not
     * already exist (the DB has a UNIQUE constraint on each FK, enforcing exactly-one-parent).
     *
     * Initial shipment status: PENDING (approved starting value — ERD §15.7).
     *
     * DEC-008 APPROVED: no carrier API integration; carrierName/trackingReference are free-text.
     *
     * @param createReq  shipment creation request
     * @return ShipmentResponse for the created shipment record
     */
    @Transactional
    public ShipmentResponse createShipment(ShipmentCreateRequest createReq) {

        if (createReq.getCustomOrderRequestId() == null && createReq.getOrderId() == null) {
            throw new IllegalArgumentException(
                    "Either customOrderRequestId or orderId must be provided");
        }
        if (createReq.getCustomOrderRequestId() != null && createReq.getOrderId() != null) {
            throw new IllegalArgumentException(
                    "Only one of customOrderRequestId or orderId may be provided — not both");
        }

        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setCarrierName(createReq.getCarrierName());
        shipment.setTrackingReference(createReq.getTrackingReference());
        shipment.setEstimatedDeliveryDate(createReq.getEstimatedDeliveryDate());

        if (createReq.getCustomOrderRequestId() != null) {
            CustomOrderRequest req = requestRepository
                    .findById(createReq.getCustomOrderRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Custom request not found: " + createReq.getCustomOrderRequestId()));
            // Enforce exactly-one-shipment-per-request constraint at service level
            if (shipmentRepository.findByCustomOrderRequestId(req.getId()).isPresent()) {
                throw new InvalidWorkflowTransitionException(
                        "A shipment already exists for custom request " + req.getId());
            }
            shipment.setCustomOrderRequest(req);
        } else {
            CustomerOrder order = customerOrderRepository
                    .findById(createReq.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order not found: " + createReq.getOrderId()));
            if (shipmentRepository.findByOrderId(order.getId()).isPresent()) {
                throw new InvalidWorkflowTransitionException(
                        "A shipment already exists for order " + order.getId());
            }
            shipment.setOrder(order);
        }

        Shipment saved = shipmentRepository.save(shipment);
        return ShipmentResponse.from(saved);
    }

    // =========================================================================
    // PATCH /api/v1/admin/shipments/{id}/status — Admin updates shipment status
    // =========================================================================

    /**
     * Admin updates the status of an existing shipment.
     *
     * Approved transitions (ERD §15.7 / DEC-008):
     *   PENDING → SHIPPED
     *   SHIPPED → DELIVERED
     *
     * Sets shippedAt when transitioning to SHIPPED; deliveredAt when to DELIVERED.
     * If the shipment belongs to a custom request, the custom request status is
     * also advanced accordingly:
     *   SHIPPED   → custom request SHIPPED
     *   DELIVERED → custom request DELIVERED
     *
     * @param shipmentId  path variable
     * @param statusReq   new status
     * @return updated ShipmentResponse
     */
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long shipmentId,
                                                  ShipmentStatusUpdateRequest statusReq) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus next = statusReq.getStatus();
        validateShipmentTransition(current, next);

        shipment.setStatus(next);
        if (next == ShipmentStatus.SHIPPED) {
            shipment.setShippedAt(OffsetDateTime.now());
        } else if (next == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(OffsetDateTime.now());
        }

        // Advance the parent custom request status if applicable
        if (shipment.getCustomOrderRequest() != null) {
            CustomOrderRequest req = shipment.getCustomOrderRequest();
            if (next == ShipmentStatus.SHIPPED) {
                req.setStatus(CustomOrderRequestStatus.SHIPPED);
                requestRepository.save(req);
            } else if (next == ShipmentStatus.DELIVERED) {
                req.setStatus(CustomOrderRequestStatus.DELIVERED);
                requestRepository.save(req);
            }
        }

        Shipment saved = shipmentRepository.save(shipment);
        return ShipmentResponse.from(saved);
    }

    // =========================================================================
    // GET /api/v1/admin/shipments/{id} — Admin views a shipment by ID
    // =========================================================================

    /**
     * Admin retrieves a shipment record by shipment ID.
     *
     * @param shipmentId path variable
     * @return ShipmentResponse
     */
    @Transactional(readOnly = true)
    public ShipmentResponse adminGetShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        return ShipmentResponse.from(shipment);
    }

    /** Retrieve the shipment associated with a custom request. */
    @Transactional(readOnly = true)
    public ShipmentResponse adminGetShipmentByRequestId(Long requestId) {
        requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
        return shipmentRepository.findByCustomOrderRequestId(requestId)
                .map(ShipmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found for custom request " + requestId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Validate that an Admin-driven production state transition is approved.
     *
     * Approved next-states per current state (ERD §15.3):
     *   IN_PRODUCTION → COMPLETED
     *   COMPLETED     → SHIPPED
     *   SHIPPED       → DELIVERED
     */
    private void validateAdminProductionTransition(CustomOrderRequestStatus current,
                                                    CustomOrderRequestStatus next) {
        boolean valid = switch (current) {
            case IN_PRODUCTION -> next == CustomOrderRequestStatus.COMPLETED;
            case COMPLETED     -> next == CustomOrderRequestStatus.SHIPPED;
            case SHIPPED       -> next == CustomOrderRequestStatus.DELIVERED;
            default            -> false;
        };
        if (!valid) {
            throw new InvalidWorkflowTransitionException(
                    "Invalid production transition: " + current + " → " + next);
        }
    }

    /**
     * Validate that a shipment status transition is approved.
     *
     * Approved transitions (ERD §15.7):
     *   PENDING → SHIPPED
     *   SHIPPED → DELIVERED
     */
    private void validateShipmentTransition(ShipmentStatus current, ShipmentStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == ShipmentStatus.SHIPPED;
            case SHIPPED -> next == ShipmentStatus.DELIVERED;
            default      -> false;
        };
        if (!valid) {
            throw new InvalidWorkflowTransitionException(
                    "Invalid shipment transition: " + current + " → " + next);
        }
    }

    /**
     * Build a CustomArtworkRequestResponse for the given entity.
     * Re-uses service-layer pattern from CustomArtworkRequestService.
     */
    private CustomArtworkRequestResponse buildResponse(CustomOrderRequest req) {
        return com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse
                .from(req, imageRepository.findByCustomOrderRequestId(req.getId())
                        .stream()
                        .map(com.handmadeart.ecommerce.dto.customartwork.CustomOrderImageResponse::from)
                        .collect(java.util.stream.Collectors.toList()));
    }
}
