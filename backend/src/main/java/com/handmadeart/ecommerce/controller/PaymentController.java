package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer payment controller for standard ready-made orders.
 *
 * Endpoints (REST API Spec §11):
 *   POST /api/v1/orders/{id}/payments  — initiate a payment for a ready-made order
 *   GET  /api/v1/orders/{id}/payments  — retrieve safe payment records for an owned order
 *
 * Authorization:
 *   CUSTOMER role required (SecurityConfig — /api/v1/orders/**).
 *   Identity resolved from JWT via CurrentUserService; no client-supplied user IDs trusted.
 *
 * DEC-001 DEFERRED: provider-agnostic / sandbox behavior.
 * Raw card number, CVV, PIN, and provider authentication secrets are never accepted
 * or stored (REST API Spec §23, FR-PAY-04, NFR-07).
 *
 * This controller is thin: all business logic is in PaymentService.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService,
                              CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    /**
     * Initiate a payment for the authenticated customer's ready-made order.
     *
     * Method:  POST
     * Path:    /api/v1/orders/{id}/payments
     * Auth:    CUSTOMER
     * Request: PaymentInitiationRequest {paymentMethod}
     * Success: 201 Created + PaymentResponse
     * Errors:  400 missing method, 401, 403, 404 order not owned/found,
     *          409 ORDER_NOT_PAYABLE (already paid/confirmed/cancelled)
     *
     * DEC-001 sandbox: payment succeeds immediately and order moves to CONFIRMED.
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentInitiationRequest request) {

        AppUser currentUser = currentUserService.getAuthenticatedUser();
        PaymentResponse response = paymentService.initiatePayment(currentUser, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve safe payment records/status for an owned order.
     *
     * Method:  GET
     * Path:    /api/v1/orders/{id}/payments
     * Auth:    CUSTOMER
     * Success: 200 OK + PaymentResponse[]
     * Errors:  401, 403, 404 order not owned/found
     */
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(@PathVariable Long id) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        List<PaymentResponse> response = paymentService.getOrderPayments(currentUser, id);
        return ResponseEntity.ok(response);
    }
}
