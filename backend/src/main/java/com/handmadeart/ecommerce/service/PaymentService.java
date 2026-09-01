package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.exception.OrderNotPayableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for standard ready-made order payment operations.
 *
 * REST API Spec §11:
 *   POST /api/v1/orders/{id}/payments  — initiate a payment for a ready-made order
 *   GET  /api/v1/orders/{id}/payments  — retrieve safe payment records for an owned order
 *
 * DEC-001 DEFERRED: provider-agnostic / mock/sandbox behavior.
 *   The approved sandbox flow:
 *     1. Create a Payment row with status PENDING.
 *     2. Immediately simulate sandbox SUCCESS: mark payment SUCCESS, set completedAt,
 *        set providerTransactionReference to a sandbox-generated reference.
 *     3. Transition the CustomerOrder from PENDING_PAYMENT → CONFIRMED.
 *   This models the full payment-to-order lifecycle without an external provider.
 *   When DEC-001 is resolved to a real provider, step 2/3 will be replaced by the
 *   provider callback flow (POST /api/v1/payments/provider-callback).
 *
 * Payable state rule:
 *   Only orders in PENDING_PAYMENT status are payable.
 *   Any other status → 409 ORDER_NOT_PAYABLE.
 *
 * Ownership rule:
 *   Order ownership is verified via findByUserIdAndId before any payment operation.
 *   A foreign orderId returns 404 (non-disclosure).
 *
 * Amount rule:
 *   Payment amount is derived from the stored order.totalAmount.
 *   No client-supplied amount is accepted.
 *
 * No raw card number, CVV, PIN, or provider secrets are ever accepted or stored
 * (FR-PAY-04, NFR-07, REST API Spec §23).
 */
@Service
public class PaymentService {

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(CustomerOrderRepository customerOrderRepository,
                          PaymentRepository paymentRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.paymentRepository = paymentRepository;
    }

    // =========================================================================
    // POST /api/v1/orders/{id}/payments — initiate payment
    // =========================================================================

    /**
     * Initiate a payment for the authenticated customer's ready-made order.
     *
     * DEC-001 sandbox flow: payment is initiated and immediately confirmed
     * (SUCCESS) within the same transaction. Order transitions to CONFIRMED.
     *
     * @param currentUser authenticated customer
     * @param orderId     path variable; ownership verified
     * @param request     payment method label (non-sensitive); no card data
     * @return PaymentResponse for the new payment record
     */
    @Transactional
    public PaymentResponse initiatePayment(AppUser currentUser, Long orderId,
                                           PaymentInitiationRequest request) {

        // Step 1: Resolve and verify order ownership
        CustomerOrder order = customerOrderRepository
                .findByUserIdAndId(currentUser.getId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Step 2: Verify the order is payable (must be PENDING_PAYMENT)
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotPayableException(
                    "Order " + orderId + " is not payable (status: " + order.getStatus() + ")");
        }

        // Step 3: Create payment record (DEC-001 sandbox — no real provider call)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(order.getTotalAmount());           // server-authoritative amount
        payment.setPaymentMethod(request.getPaymentMethod()); // method label only; no card data

        // Step 4: Sandbox SUCCESS — immediately record outcome
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderTransactionReference("SANDBOX-" + order.getId() + "-" + System.currentTimeMillis());
        payment.setCompletedAt(OffsetDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Step 5: Transition order PENDING_PAYMENT → CONFIRMED
        order.setStatus(OrderStatus.CONFIRMED);
        customerOrderRepository.save(order);

        return PaymentResponse.from(savedPayment);
    }

    // =========================================================================
    // GET /api/v1/orders/{id}/payments — retrieve order payments
    // =========================================================================

    /**
     * Retrieve all safe payment records for an order owned by the authenticated customer.
     *
     * Ownership is enforced: a foreign orderId returns 404 (non-disclosure).
     * Includes all payment rows (PENDING, SUCCESS, FAILED) for audit history.
     *
     * @param currentUser authenticated customer
     * @param orderId     path variable; ownership verified
     * @return list of payment records for the order
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(AppUser currentUser, Long orderId) {
        // Verify order ownership (non-disclosure: foreign orderId → 404)
        customerOrderRepository
                .findByUserIdAndId(currentUser.getId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
}
