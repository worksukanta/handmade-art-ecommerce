package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomRequestReviewRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentStatusUpdateRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.service.AdminProductionService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.CustomArtworkRequestService;
import com.handmadeart.ecommerce.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin custom artwork management controller.
 *
 * Endpoints (REST API Spec §13, §14 admin, §15 shipping):
 *   GET   /api/v1/admin/custom-requests                   — list all custom requests
 *   PATCH /api/v1/admin/custom-requests/{id}/review       — review a request
 *   POST  /api/v1/admin/custom-requests/{id}/quotation    — create a quotation
 *   GET   /api/v1/admin/quotations/{id}                   — get a quotation
 *   PATCH /api/v1/admin/custom-requests/{id}/status       — update production status
 *   POST  /api/v1/admin/shipments                         — create a shipment record
 *   PATCH /api/v1/admin/shipments/{id}/status             — update shipment status
 *   GET   /api/v1/admin/shipments/{id}                    — get shipment by ID
 *
 * Authorization:
 *   ADMIN role required for all endpoints (enforced by SecurityConfig: /api/v1/admin/**).
 *   Identity resolved from JWT via CurrentUserService (used for reviewedBy / createdBy audit).
 *   CUSTOMER attempting admin endpoints → 403 (SecurityConfig route rule).
 *
 * This controller is thin: all business logic is in the service layer.
 */
@RestController
public class AdminCustomArtworkController {

    private final CustomArtworkRequestService customArtworkRequestService;
    private final QuotationService quotationService;
    private final AdminProductionService adminProductionService;
    private final CurrentUserService currentUserService;

    public AdminCustomArtworkController(CustomArtworkRequestService customArtworkRequestService,
                                         QuotationService quotationService,
                                         AdminProductionService adminProductionService,
                                         CurrentUserService currentUserService) {
        this.customArtworkRequestService = customArtworkRequestService;
        this.quotationService = quotationService;
        this.adminProductionService = adminProductionService;
        this.currentUserService = currentUserService;
    }

    /**
     * List all custom requests with optional status filter.
     *
     * Method:  GET
     * Path:    /api/v1/admin/custom-requests
     * Auth:    ADMIN
     * Params:  status (optional), page (default 0), size (default 20)
     * Success: 200 OK + PageResponse&lt;CustomArtworkRequestSummary&gt;
     * Errors:  401, 403
     */
    @GetMapping("/api/v1/admin/custom-requests")
    public ResponseEntity<PageResponse<CustomArtworkRequestSummary>> adminListCustomRequests(
            @RequestParam(required = false) CustomOrderRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<CustomArtworkRequestSummary> response =
                customArtworkRequestService.adminListCustomRequests(status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Record an Admin review decision on a custom artwork request.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/custom-requests/{id}/review
     * Auth:    ADMIN
     * Request: CustomRequestReviewRequest {decision, notes?}
     * Success: 200 OK + CustomArtworkRequestResponse
     * Errors:  400 invalid decision, 401, 403, 404, 409 invalid transition
     */
    @PatchMapping("/api/v1/admin/custom-requests/{id}/review")
    public ResponseEntity<CustomArtworkRequestResponse> adminReviewCustomRequest(
            @PathVariable Long id,
            @Valid @RequestBody CustomRequestReviewRequest reviewRequest) {

        AppUser adminUser = currentUserService.getAuthenticatedUser();
        CustomArtworkRequestResponse response =
                customArtworkRequestService.adminReviewCustomRequest(adminUser, id, reviewRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a quotation for a custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/admin/custom-requests/{id}/quotation
     * Auth:    ADMIN
     * Request: QuotationCreateRequest
     * Success: 201 Created + QuotationResponse
     * Errors:  400 validation/expiry in past, 401, 403, 404, 409 ineligible state or duplicate
     */
    @PostMapping("/api/v1/admin/custom-requests/{id}/quotation")
    public ResponseEntity<QuotationResponse> createQuotation(
            @PathVariable Long id,
            @Valid @RequestBody QuotationCreateRequest createRequest) {

        AppUser adminUser = currentUserService.getAuthenticatedUser();
        QuotationResponse response = quotationService.createQuotation(adminUser, id, createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a quotation directly by its quotation ID.
     *
     * Method:  GET
     * Path:    /api/v1/admin/quotations/{id}
     * Auth:    ADMIN
     * Success: 200 OK + QuotationResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/api/v1/admin/quotations/{id}")
    public ResponseEntity<QuotationResponse> adminGetQuotation(@PathVariable Long id) {
        QuotationResponse response = quotationService.adminGetQuotation(id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PATCH /api/v1/admin/custom-requests/{id}/status — production status update
    // =========================================================================

    /**
     * Update the production status of a custom artwork request.
     *
     * Approved Admin transitions only (ERD §15.3):
     *   IN_PRODUCTION → COMPLETED
     *   COMPLETED     → SHIPPED
     *   SHIPPED       → DELIVERED
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/custom-requests/{id}/status
     * Auth:    ADMIN
     * Request: { "status": "COMPLETED" }
     * Success: 200 OK + CustomArtworkRequestResponse
     * Errors:  400 invalid status value, 401, 403, 404, 409 invalid transition
     */
    @PatchMapping("/api/v1/admin/custom-requests/{id}/status")
    public ResponseEntity<CustomArtworkRequestResponse> updateCustomRequestStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        CustomOrderRequestStatus newStatus;
        try {
            newStatus = CustomOrderRequestStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + statusStr);
        }

        CustomArtworkRequestResponse response =
                adminProductionService.updateCustomRequestStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/v1/admin/shipments — Admin creates a shipment record
    // =========================================================================

    /**
     * Create a shipment record for a custom request or ready-made order.
     *
     * Method:  POST
     * Path:    /api/v1/admin/shipments
     * Auth:    ADMIN
     * Request: ShipmentCreateRequest
     * Success: 201 Created + ShipmentResponse
     * Errors:  400 invalid request, 401, 403, 404 parent not found, 409 duplicate
     *
     * DEC-008 APPROVED: no carrier API; free-text carrier name/tracking reference.
     */
    @PostMapping("/api/v1/admin/shipments")
    public ResponseEntity<ShipmentResponse> createShipment(
            @RequestBody ShipmentCreateRequest createRequest) {

        ShipmentResponse response = adminProductionService.createShipment(createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // PATCH /api/v1/admin/shipments/{id}/status — Admin updates shipment status
    // =========================================================================

    /**
     * Update the status of a shipment.
     *
     * Approved transitions (ERD §15.7):
     *   PENDING → SHIPPED
     *   SHIPPED → DELIVERED
     *
     * Sets shippedAt/deliveredAt timestamps automatically.
     * Also advances the parent custom request status if applicable.
     *
     * Method:  PATCH
     * Path:    /api/v1/admin/shipments/{id}/status
     * Auth:    ADMIN
     * Request: ShipmentStatusUpdateRequest {status}
     * Success: 200 OK + ShipmentResponse
     * Errors:  400 missing status, 401, 403, 404, 409 invalid transition
     */
    @PatchMapping("/api/v1/admin/shipments/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest statusRequest) {

        ShipmentResponse response = adminProductionService.updateShipmentStatus(id, statusRequest);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/v1/admin/shipments/{id} — Admin views a shipment by ID
    // =========================================================================

    /**
     * Retrieve a shipment record by its ID (Admin only).
     *
     * Method:  GET
     * Path:    /api/v1/admin/shipments/{id}
     * Auth:    ADMIN
     * Success: 200 OK + ShipmentResponse
     * Errors:  401, 403, 404
     */
    @GetMapping("/api/v1/admin/shipments/{id}")
    public ResponseEntity<ShipmentResponse> adminGetShipment(@PathVariable Long id) {
        ShipmentResponse response = adminProductionService.adminGetShipment(id);
        return ResponseEntity.ok(response);
    }
}
