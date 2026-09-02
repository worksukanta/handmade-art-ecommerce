package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.CustomOrderImageResponse;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.CustomAdvancePaymentService;
import com.handmadeart.ecommerce.service.CustomArtworkRequestService;
import com.handmadeart.ecommerce.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Customer-facing custom artwork request controller.
 *
 * Endpoints (REST API Spec §13, §14 customer read, §14 approval/rejection, §13 advance payment):
 *   POST  /api/v1/custom-requests                    — submit a custom artwork request
 *   GET   /api/v1/custom-requests                    — list authenticated customer's requests
 *   GET   /api/v1/custom-requests/{id}               — get detail of an owned request
 *   POST  /api/v1/custom-requests/{id}/images        — upload a reference image
 *   GET   /api/v1/custom-requests/{id}/quotation     — view quotation for an owned request
 *   POST  /api/v1/quotations/{id}/approve            — customer approves quotation
 *   POST  /api/v1/quotations/{id}/reject             — customer rejects quotation
 *   POST  /api/v1/custom-requests/{id}/payments      — initiate advance payment (DEC-005)
 *   GET   /api/v1/custom-requests/{id}/payments      — get advance payment records
 *   GET   /api/v1/custom-requests/{id}/shipment      — view shipment/tracking
 *
 * Authorization:
 *   CUSTOMER role required (enforced by SecurityConfig: /api/v1/custom-requests/**).
 *   The /api/v1/quotations/** endpoints are also CUSTOMER-only (SecurityConfig updated).
 *   Identity resolved from JWT via CurrentUserService.
 *   No client-supplied user IDs are trusted.
 *
 * Ownership enforcement:
 *   All customer operations are scoped to the authenticated user via CurrentUserService.
 *   A foreign or missing id returns 404 (non-disclosure semantics).
 */
@RestController
public class CustomArtworkController {

    private final CustomArtworkRequestService customArtworkRequestService;
    private final QuotationService quotationService;
    private final CustomAdvancePaymentService advancePaymentService;
    private final CurrentUserService currentUserService;

    public CustomArtworkController(CustomArtworkRequestService customArtworkRequestService,
                                    QuotationService quotationService,
                                    CustomAdvancePaymentService advancePaymentService,
                                    CurrentUserService currentUserService) {
        this.customArtworkRequestService = customArtworkRequestService;
        this.quotationService = quotationService;
        this.advancePaymentService = advancePaymentService;
        this.currentUserService = currentUserService;
    }

    /**
     * Submit a new custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/custom-requests
     * Auth:    CUSTOMER
     * Request: CustomArtworkRequestCreateRequest (validated)
     * Success: 201 Created + CustomArtworkRequestResponse
     * Errors:  400 validation, 401, 403
     */
    @PostMapping("/api/v1/custom-requests")
    public ResponseEntity<CustomArtworkRequestResponse> createCustomRequest(
            @Valid @RequestBody CustomArtworkRequestCreateRequest request) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomArtworkRequestResponse response =
                customArtworkRequestService.createCustomRequest(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all custom requests for the authenticated customer.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests
     * Auth:    CUSTOMER
     * Params:  page (default 0), size (default 20), status (optional filter)
     * Success: 200 OK + PageResponse&lt;CustomArtworkRequestSummary&gt;
     * Errors:  401, 403
     */
    @GetMapping("/api/v1/custom-requests")
    public ResponseEntity<PageResponse<CustomArtworkRequestSummary>> listCustomRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CustomOrderRequestStatus status) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        PageResponse<CustomArtworkRequestSummary> response =
                customArtworkRequestService.listCustomRequests(currentUser, page, size, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detail of a single custom artwork request owned by the authenticated customer.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}
     * Auth:    CUSTOMER
     * Success: 200 OK + CustomArtworkRequestResponse
     * Errors:  401, 403, 404 (foreign or missing id)
     */
    @GetMapping("/api/v1/custom-requests/{id}")
    public ResponseEntity<CustomArtworkRequestResponse> getCustomRequest(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomArtworkRequestResponse response =
                customArtworkRequestService.getCustomRequest(currentUser, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload a reference image for an owned custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/custom-requests/{id}/images
     * Auth:    CUSTOMER
     * Request: multipart/form-data; field name "file"
     * Success: 201 Created + CustomOrderImageResponse
     * Errors:  400 invalid type/empty, 401, 403, 404, 413 size (DEC-003 OPEN)
     *
     * DEC-003 OPEN: size limit not enforced; only content-type image/* validated.
     */
    @PostMapping("/api/v1/custom-requests/{id}/images")
    public ResponseEntity<CustomOrderImageResponse> uploadReferenceImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        CustomOrderImageResponse response =
                customArtworkRequestService.uploadReferenceImage(currentUser, id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * View the quotation for an owned custom artwork request.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}/quotation
     * Auth:    CUSTOMER
     * Success: 200 OK + QuotationResponse
     * Errors:  401, 403, 404 (foreign/missing request or no quotation yet)
     */
    @GetMapping("/api/v1/custom-requests/{id}/quotation")
    public ResponseEntity<QuotationResponse> getQuotation(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        QuotationResponse response = quotationService.customerGetQuotation(currentUser, id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/v1/quotations/{id}/approve — Customer approves quotation
    // =========================================================================

    /**
     * Approve a quotation for an owned custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/quotations/{id}/approve
     * Auth:    CUSTOMER
     * Success: 200 OK + QuotationResponse (status = APPROVED)
     * Errors:  401, 403, 404 (foreign/missing), 409 invalid state or expired
     */
    @PostMapping("/api/v1/quotations/{id}/approve")
    public ResponseEntity<QuotationResponse> approveQuotation(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        QuotationResponse response = quotationService.approveQuotation(currentUser, id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/v1/quotations/{id}/reject — Customer rejects quotation
    // =========================================================================

    /**
     * Reject a quotation for an owned custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/quotations/{id}/reject
     * Auth:    CUSTOMER
     * Success: 200 OK + QuotationResponse (status = REJECTED)
     * Errors:  401, 403, 404 (foreign/missing), 409 invalid state
     */
    @PostMapping("/api/v1/quotations/{id}/reject")
    public ResponseEntity<QuotationResponse> rejectQuotation(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        QuotationResponse response = quotationService.rejectQuotation(currentUser, id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/v1/custom-requests/{id}/payments — Initiate advance payment
    // =========================================================================

    /**
     * Initiate the advance payment for an approved custom artwork request.
     *
     * Method:  POST
     * Path:    /api/v1/custom-requests/{id}/payments
     * Auth:    CUSTOMER
     * Request: PaymentInitiationRequest (payment method label; no card data)
     * Success: 201 Created + PaymentResponse
     * Errors:  400 missing method, 401, 403, 404 foreign/missing, 409 invalid state
     *
     * DEC-005 APPROVED: authoritative amount = stored Quotation.advancePaymentAmount.
     * DEC-001 DEFERRED: sandbox/mock payment flow.
     */
    @PostMapping("/api/v1/custom-requests/{id}/payments")
    public ResponseEntity<PaymentResponse> initiateAdvancePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentInitiationRequest request) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        PaymentResponse response = advancePaymentService.initiateAdvancePayment(currentUser, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id}/payments — Get advance payment records
    // =========================================================================

    /**
     * Retrieve advance payment records for an owned custom artwork request.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}/payments
     * Auth:    CUSTOMER
     * Success: 200 OK + PaymentResponse[]
     * Errors:  401, 403, 404 foreign/missing
     */
    @GetMapping("/api/v1/custom-requests/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getCustomRequestPayments(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        List<PaymentResponse> response = advancePaymentService.getCustomRequestPayments(currentUser, id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/v1/custom-requests/{id}/shipment — Customer views own shipment
    // =========================================================================

    /**
     * View shipment/tracking information for an owned custom artwork request.
     *
     * Method:  GET
     * Path:    /api/v1/custom-requests/{id}/shipment
     * Auth:    CUSTOMER
     * Success: 200 OK + ShipmentResponse
     * Errors:  401, 403, 404 (foreign/missing request or no shipment yet)
     */
    @GetMapping("/api/v1/custom-requests/{id}/shipment")
    public ResponseEntity<com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse> getCustomRequestShipment(
            @PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse response =
                advancePaymentService.getCustomRequestShipment(currentUser, id);
        return ResponseEntity.ok(response);
    }
}
