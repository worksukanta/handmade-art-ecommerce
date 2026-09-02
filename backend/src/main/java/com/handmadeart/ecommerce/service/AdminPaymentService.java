package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.order.AdminPaymentResponse;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for admin payment management.
 *
 * REST API Spec §12: GET /api/v1/admin/payments/{id}.
 *
 * Admin read-only access to any payment record regardless of whether it is
 * associated with a ready-made order or a custom artwork request.
 *
 * Security: never exposes raw card number, CVV, PIN, or provider secrets.
 * Provider callback (POST /payments/provider-callback) is NOT implemented —
 * blocked by DEC-001 (payment provider selection DEFERRED).
 */
@Service
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    public AdminPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Return admin payment detail for any payment record by id.
     *
     * @param paymentId path variable
     * @return AdminPaymentResponse (no sensitive credentials)
     */
    public AdminPaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return AdminPaymentResponse.from(payment);
    }
}
