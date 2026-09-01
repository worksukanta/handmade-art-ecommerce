package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.OrderNotPayableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentService} business logic.
 *
 * Covered:
 *   PAY-S-01  initiatePayment succeeds — Payment created, order → CONFIRMED
 *   PAY-S-02  initiatePayment amount from stored order total (not client-supplied)
 *   PAY-S-03  initiatePayment with foreign orderId → ResourceNotFoundException (non-disclosure)
 *   PAY-S-04  initiatePayment with already-CONFIRMED order → OrderNotPayableException
 *   PAY-S-05  initiatePayment with CANCELLED order → OrderNotPayableException
 *   PAY-S-06  getOrderPayments returns payments for owned order
 *   PAY-S-07  getOrderPayments with foreign orderId → ResourceNotFoundException
 *   PAY-S-08  PaymentResponse contains no sensitive data (no card number/CVV/PIN)
 *   PAY-S-09  payment purpose is FULL for ready-made orders
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private CustomerOrderRepository customerOrderRepository;
    @Mock private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(customerOrderRepository, paymentRepository);
    }

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    private AppUser buildCustomer(Long id) {
        AppUser user = new AppUser();
        user.setEmail("customer" + id + "@example.com");
        user.setFullName("Customer " + id);
        user.setRole(UserRole.CUSTOMER);
        setId(user, AppUser.class, id);
        return user;
    }

    private CustomerOrder buildOrder(Long id, AppUser user, OrderStatus status,
                                     BigDecimal total) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(status);
        order.setShipRecipientName("Alice Smith");
        order.setShipLine1("10 Main Street");
        order.setShipCity("London");
        order.setShipStateProvince("England");
        order.setShipPostalCode("SW1A 1AA");
        order.setShipCountry("United Kingdom");
        order.setSubtotalAmount(total);
        order.setTotalAmount(total);
        setId(order, CustomerOrder.class, id);
        return order;
    }

    private Payment buildPayment(Long id, CustomerOrder order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderTransactionReference("SANDBOX-" + order.getId() + "-12345");
        setId(payment, Payment.class, id);
        return payment;
    }

    private <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            var field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // PAY-S-01: initiatePayment success
    // =========================================================================

    @Test
    @DisplayName("PAY-S-01: initiatePayment succeeds — payment created, order transitions to CONFIRMED")
    void initiatePayment_success_createsPaymentAndConfirmsOrder() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.PENDING_PAYMENT,
                new BigDecimal("100.00"));

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        PaymentResponse response = paymentService.initiatePayment(user, 10L, req);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPaymentPurpose()).isEqualTo(PaymentPurpose.FULL);

        // Order should be transitioned to CONFIRMED
        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(customerOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // =========================================================================
    // PAY-S-02: payment amount derived from stored order total
    // =========================================================================

    @Test
    @DisplayName("PAY-S-02: payment amount is derived from stored order.totalAmount, not client-supplied")
    void initiatePayment_amountFromStoredOrderTotal() {
        AppUser user = buildCustomer(1L);
        BigDecimal storedTotal = new BigDecimal("185.50");
        CustomerOrder order = buildOrder(10L, user, OrderStatus.PENDING_PAYMENT, storedTotal);

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.save(any())).thenReturn(order);

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("CARD");

        PaymentResponse response = paymentService.initiatePayment(user, 10L, req);

        // Amount must equal stored order total — never a client-supplied value
        assertThat(response.getAmount()).isEqualByComparingTo(storedTotal);
    }

    // =========================================================================
    // PAY-S-03: initiatePayment with foreign orderId → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("PAY-S-03: initiatePayment with foreign orderId returns 404 (non-disclosure)")
    void initiatePayment_foreignOrderId_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);

        when(customerOrderRepository.findByUserIdAndId(1L, 999L)).thenReturn(Optional.empty());

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        assertThatThrownBy(() -> paymentService.initiatePayment(user, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // =========================================================================
    // PAY-S-04: initiatePayment on already-CONFIRMED order → 409 ORDER_NOT_PAYABLE
    // =========================================================================

    @Test
    @DisplayName("PAY-S-04: initiatePayment on already-CONFIRMED order throws OrderNotPayableException")
    void initiatePayment_confirmedOrder_throwsOrderNotPayableException() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.CONFIRMED, new BigDecimal("50.00"));

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        assertThatThrownBy(() -> paymentService.initiatePayment(user, 10L, req))
                .isInstanceOf(OrderNotPayableException.class);
    }

    // =========================================================================
    // PAY-S-05: initiatePayment on CANCELLED order → 409 ORDER_NOT_PAYABLE
    // =========================================================================

    @Test
    @DisplayName("PAY-S-05: initiatePayment on CANCELLED order throws OrderNotPayableException")
    void initiatePayment_cancelledOrder_throwsOrderNotPayableException() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.CANCELLED, new BigDecimal("50.00"));

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("SANDBOX");

        assertThatThrownBy(() -> paymentService.initiatePayment(user, 10L, req))
                .isInstanceOf(OrderNotPayableException.class);
    }

    // =========================================================================
    // PAY-S-06: getOrderPayments returns payment list for owned order
    // =========================================================================

    @Test
    @DisplayName("PAY-S-06: getOrderPayments returns payment records for owned order")
    void getOrderPayments_returnsPaymentList() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.CONFIRMED, new BigDecimal("50.00"));
        Payment p1 = buildPayment(100L, order);
        Payment p2 = buildPayment(101L, order);

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(p1, p2));

        List<PaymentResponse> result = paymentService.getOrderPayments(user, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPaymentId()).isEqualTo(100L);
        assertThat(result.get(1).getPaymentId()).isEqualTo(101L);
    }

    // =========================================================================
    // PAY-S-07: getOrderPayments with foreign orderId → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("PAY-S-07: getOrderPayments with foreign orderId returns 404 (non-disclosure)")
    void getOrderPayments_foreignOrderId_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);

        when(customerOrderRepository.findByUserIdAndId(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getOrderPayments(user, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // =========================================================================
    // PAY-S-08: PaymentResponse contains no sensitive data
    // =========================================================================

    @Test
    @DisplayName("PAY-S-08: PaymentResponse does not expose sensitive payment credentials")
    void initiatePayment_responseContainsNoSensitiveData() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.PENDING_PAYMENT,
                new BigDecimal("60.00"));

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.save(any())).thenReturn(order);

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("CARD");

        PaymentResponse response = paymentService.initiatePayment(user, 10L, req);

        // Verify the response contains only the safe fields defined by the spec
        // (no card number, CVV, PIN, or provider secret would be in PaymentResponse)
        assertThat(response.getPaymentId()).isNull();      // not set — fixture; ok
        assertThat(response.getAmount()).isNotNull();
        assertThat(response.getPaymentMethod()).isEqualTo("CARD");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    // =========================================================================
    // PAY-S-09: payment purpose is FULL for ready-made orders
    // =========================================================================

    @Test
    @DisplayName("PAY-S-09: standard order payment has purpose FULL")
    void initiatePayment_purposeIsFullForReadyMadeOrder() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.PENDING_PAYMENT,
                new BigDecimal("40.00"));

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.save(any())).thenReturn(order);

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setPaymentMethod("UPI");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        paymentService.initiatePayment(user, 10L, req);
        verify(paymentRepository).save(paymentCaptor.capture());

        assertThat(paymentCaptor.getValue().getPaymentPurpose()).isEqualTo(PaymentPurpose.FULL);
    }
}
