package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomRequestReviewRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
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
 * Endpoints (REST API Spec §13, §14 admin):
 *   GET   /api/v1/admin/custom-requests                — list all custom requests (filterable)
 *   PATCH /api/v1/admin/custom-requests/{id}/review    — review a request (accept/reject)
 *   POST  /api/v1/admin/custom-requests/{id}/quotation — create a quotation
 *   GET   /api/v1/admin/quotations/{id}                — get a quotation by quotation ID
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
    private final CurrentUserService currentUserService;

    public AdminCustomArtworkController(CustomArtworkRequestService customArtworkRequestService,
                                         QuotationService quotationService,
                                         CurrentUserService currentUserService) {
        this.customArtworkRequestService = customArtworkRequestService;
        this.quotationService = quotationService;
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
}
