package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.customartwork.ShipmentResponse;
import com.handmadeart.ecommerce.dto.customartwork.ShipmentStatusUpdateRequest;
import com.handmadeart.ecommerce.dto.order.PaymentInitiationRequest;
import com.handmadeart.ecommerce.dto.order.PaymentResponse;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.entity.ShipmentStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import com.handmadeart.ecommerce.service.AdminProductionService;
import com.handmadeart.ecommerce.service.CustomAdvancePaymentService;
import com.handmadeart.ecommerce.service.QuotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Phase 3E.2 services.
 *
 * Covered (QuotationService — approval/rejection):
 *   QUO-S-11  approveQuotation: customer approves own PENDING quotation → APPROVED
 *   QUO-S-12  approveQuotation: foreign quotation → ResourceNotFoundException (non-disclosure)
 *   QUO-S-13  approveQuotation: already APPROVED quotation → InvalidWorkflowTransitionException
 *   QUO-S-14  approveQuotation: expired quotation → InvalidWorkflowTransitionException
 *   QUO-S-15  approveQuotation: transitions request status QUOTED → APPROVED
 *   QUO-S-16  rejectQuotation: customer rejects own PENDING quotation → REJECTED
 *   QUO-S-17  rejectQuotation: transitions request status QUOTED → REJECTED
 *   QUO-S-18  rejectQuotation: already REJECTED quotation → InvalidWorkflowTransitionException
 *
 * Covered (CustomAdvancePaymentService):
 *   ADV-S-01  initiateAdvancePayment: succeeds when request is APPROVED
 *   ADV-S-02  initiateAdvancePayment: amount from quotation advanceAmount (DEC-005)
 *   ADV-S-03  initiateAdvancePayment: transitions request → IN_PRODUCTION (sandbox)
 *   ADV-S-04  initiateAdvancePayment: request not in APPROVED state → 409
 *   ADV-S-05  initiateAdvancePayment: duplicate successful payment → 409
 *   ADV-S-06  initiateAdvancePayment: foreign request → 404 (non-disclosure)
 *   ADV-S-07  getCustomRequestPayments: returns payments for owned request
 *   ADV-S-08  getCustomRequestPayments: foreign request → 404 (non-disclosure)
 *   ADV-S-09  getCustomRequestShipment: returns shipment for owned request
 *   ADV-S-10  getCustomRequestShipment: no shipment → 404
 *   ADV-S-11  getCustomRequestShipment: cross-customer request → 404 (non-disclosure)
 *
 * Covered (AdminProductionService):
 *   PROD-S-01  updateCustomRequestStatus: IN_PRODUCTION → COMPLETED (valid)
 *   PROD-S-02  updateCustomRequestStatus: COMPLETED → SHIPPED (valid)
 *   PROD-S-03  updateCustomRequestStatus: invalid transition → InvalidWorkflowTransitionException
 *   PROD-S-04  createShipment: creates shipment for custom request with PENDING status
 *   PROD-S-05  createShipment: duplicate shipment for same request → 409
 *   PROD-S-06  updateShipmentStatus: PENDING → SHIPPED; sets shippedAt + advances custom request
 *   PROD-S-07  updateShipmentStatus: SHIPPED → DELIVERED; sets deliveredAt + advances custom request
 *   PROD-S-08  updateShipmentStatus: invalid transition → InvalidWorkflowTransitionException
 */
@ExtendWith(MockitoExtension.class)
class CustomArtworkPhase2ServiceTest {

    @Mock private QuotationRepository quotationRepository;
    @Mock private CustomOrderRequestRepository requestRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private CustomerOrderRepository customerOrderRepository;
    @Mock private CustomOrderImageRepository imageRepository;

    private QuotationService quotationService;
    private CustomAdvancePaymentService advancePaymentService;
    private AdminProductionService adminProductionService;

    @BeforeEach
    void setUp() {
        quotationService = new QuotationService(quotationRepository, requestRepository);
        advancePaymentService = new CustomAdvancePaymentService(
                requestRepository, quotationRepository, paymentRepository, shipmentRepository);
        adminProductionService = new AdminProductionService(
                requestRepository, customerOrderRepository, imageRepository, shipmentRepository);
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    private AppUser buildCustomer(Long id) {
        AppUser user = new AppUser();
        user.setEmail("customer" + id + "@example.com");
        user.setFullName("Customer " + id);
        user.setRole(UserRole.CUSTOMER);
        setId(user, AppUser.class, id);
        return user;
    }

    private AppUser buildAdmin(Long id) {
        AppUser user = new AppUser();
        user.setEmail("admin" + id + "@example.com");
        user.setFullName("Admin " + id);
        user.setRole(UserRole.ADMIN);
        setId(user, AppUser.class, id);
        return user;
    }

    private CustomOrderRequest buildRequest(Long id, AppUser owner, CustomOrderRequestStatus status) {
        CustomOrderRequest req = new CustomOrderRequest();
        req.setUser(owner);
        req.setProductType("Oil Painting");
        req.setDescription("A custom oil painting");
        req.setStatus(status);
        setId(req, CustomOrderRequest.class, id);
        return req;
    }

    private Quotation buildQuotation(Long id, CustomOrderRequest req, QuotationStatus status,
                                      OffsetDateTime expiryAt, BigDecimal advanceAmount) {
        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("500.00"));
        q.setAdvanceAmount(advanceAmount);
        q.setStatus(status);
        q.setExpiryAt(expiryAt);
        q.setCreatedBy(buildAdmin(99L));
        setId(q, Quotation.class, id);
        return q;
    }

    private Payment buildPayment(Long id, CustomOrderRequest req, PaymentPurpose purpose,
                                  PaymentStatus status) {
        Payment p = new Payment();
        p.setCustomOrderRequest(req);
        p.setPaymentPurpose(purpose);
        p.setAmount(new BigDecimal("150.00"));
        p.setPaymentMethod("CARD");
        p.setStatus(status);
        setId(p, Payment.class, id);
        return p;
    }

    private Shipment buildShipment(Long id, CustomOrderRequest req, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setCustomOrderRequest(req);
        s.setStatus(status);
        setId(s, Shipment.class, id);
        return s;
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
    // QUO-S-11: approveQuotation — customer approves own PENDING quotation
    // =========================================================================

    @Test
    @DisplayName("QUO-S-11: approveQuotation succeeds for own PENDING quotation")
    void approveQuotation_ownPendingQuotation_succeeds() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.PENDING,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        QuotationResponse response = quotationService.approveQuotation(customer, 100L);

        assertThat(response.getStatus()).isEqualTo(QuotationStatus.APPROVED);
    }

    // =========================================================================
    // QUO-S-12: approveQuotation — foreign quotation → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("QUO-S-12: approveQuotation for foreign customer's quotation returns 404 (non-disclosure)")
    void approveQuotation_foreignQuotation_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, foreignReq, QuotationStatus.PENDING,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> quotationService.approveQuotation(customer, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // QUO-S-13: approveQuotation — already APPROVED → InvalidWorkflowTransitionException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-13: approveQuotation on already-APPROVED quotation → InvalidWorkflowTransitionException")
    void approveQuotation_alreadyApproved_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.APPROVED,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> quotationService.approveQuotation(customer, 100L))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("PENDING");
    }

    // =========================================================================
    // QUO-S-14: approveQuotation — expired quotation → InvalidWorkflowTransitionException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-14: approveQuotation on expired quotation → InvalidWorkflowTransitionException")
    void approveQuotation_expiredQuotation_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.PENDING,
                OffsetDateTime.now().minusDays(1), // past expiry
                new BigDecimal("150.00"));

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> quotationService.approveQuotation(customer, 100L))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("expired");
    }

    // =========================================================================
    // QUO-S-15: approveQuotation — transitions request QUOTED → APPROVED
    // =========================================================================

    @Test
    @DisplayName("QUO-S-15: approveQuotation transitions request status QUOTED → APPROVED")
    void approveQuotation_transitionsRequestToApproved() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.PENDING,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(CustomOrderRequestStatus.APPROVED);
            return saved;
        });

        quotationService.approveQuotation(customer, 100L);
        verify(requestRepository).save(any(CustomOrderRequest.class));
    }

    // =========================================================================
    // QUO-S-16: rejectQuotation — customer rejects own PENDING quotation
    // =========================================================================

    @Test
    @DisplayName("QUO-S-16: rejectQuotation succeeds for own PENDING quotation")
    void rejectQuotation_ownPendingQuotation_succeeds() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.PENDING,
                OffsetDateTime.now().plusDays(7), null);

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        QuotationResponse response = quotationService.rejectQuotation(customer, 100L);

        assertThat(response.getStatus()).isEqualTo(QuotationStatus.REJECTED);
    }

    // =========================================================================
    // QUO-S-17: rejectQuotation — transitions request QUOTED → REJECTED
    // =========================================================================

    @Test
    @DisplayName("QUO-S-17: rejectQuotation transitions request status QUOTED → REJECTED")
    void rejectQuotation_transitionsRequestToRejected() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.PENDING,
                OffsetDateTime.now().plusDays(7), null);

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(CustomOrderRequestStatus.REJECTED);
            return saved;
        });

        quotationService.rejectQuotation(customer, 100L);
        verify(requestRepository).save(any(CustomOrderRequest.class));
    }

    // =========================================================================
    // QUO-S-18: rejectQuotation — already REJECTED → InvalidWorkflowTransitionException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-18: rejectQuotation on already-REJECTED quotation → InvalidWorkflowTransitionException")
    void rejectQuotation_alreadyRejected_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REJECTED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.REJECTED,
                OffsetDateTime.now().plusDays(7), null);

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> quotationService.rejectQuotation(customer, 100L))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("PENDING");
    }

    // =========================================================================
    // ADV-S-01: initiateAdvancePayment — succeeds when request is APPROVED
    // =========================================================================

    @Test
    @DisplayName("ADV-S-01: initiateAdvancePayment succeeds when request is APPROVED")
    void initiateAdvancePayment_approvedRequest_succeeds() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.APPROVED,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(q));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            setId(p, Payment.class, 200L);
            return p;
        });
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = advancePaymentService.initiateAdvancePayment(customer, 10L, payReq);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPaymentPurpose()).isEqualTo(PaymentPurpose.ADVANCE);
    }

    // =========================================================================
    // ADV-S-02: initiateAdvancePayment — amount from quotation advanceAmount (DEC-005)
    // =========================================================================

    @Test
    @DisplayName("ADV-S-02: initiateAdvancePayment uses quotation advanceAmount as authoritative amount (DEC-005)")
    void initiateAdvancePayment_usesQuotationAdvanceAmount() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        BigDecimal expectedAmount = new BigDecimal("150.00");
        Quotation q = buildQuotation(100L, req, QuotationStatus.APPROVED,
                OffsetDateTime.now().plusDays(7), expectedAmount);

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(q));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            // Verify amount equals the stored quotation advance amount
            assertThat(p.getAmount()).isEqualByComparingTo(expectedAmount);
            setId(p, Payment.class, 200L);
            return p;
        });
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        advancePaymentService.initiateAdvancePayment(customer, 10L, payReq);
    }

    // =========================================================================
    // ADV-S-03: initiateAdvancePayment — transitions request → IN_PRODUCTION
    // =========================================================================

    @Test
    @DisplayName("ADV-S-03: initiateAdvancePayment (sandbox) transitions request to IN_PRODUCTION")
    void initiateAdvancePayment_transitionsRequestToInProduction() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.APPROVED,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(q));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            setId((Payment) inv.getArgument(0), Payment.class, 200L);
            return inv.getArgument(0);
        });
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(CustomOrderRequestStatus.IN_PRODUCTION);
            return saved;
        });

        advancePaymentService.initiateAdvancePayment(customer, 10L, payReq);
        verify(requestRepository).save(any(CustomOrderRequest.class));
    }

    // =========================================================================
    // ADV-S-02b: initiateAdvancePayment — PaymentResponse includes customOrderRequestId
    // =========================================================================

    @Test
    @DisplayName("ADV-S-02b: initiateAdvancePayment response includes customOrderRequestId (not null)")
    void initiateAdvancePayment_responseIncludesCustomOrderRequestId() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        Quotation q = buildQuotation(100L, req, QuotationStatus.APPROVED,
                OffsetDateTime.now().plusDays(7), new BigDecimal("150.00"));

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(q));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            setId(p, Payment.class, 200L);
            return p;
        });
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = advancePaymentService.initiateAdvancePayment(customer, 10L, payReq);

        // customOrderRequestId must be populated (fix: PaymentResponse now maps customOrderRequest)
        assertThat(response.getCustomOrderRequestId()).isEqualTo(10L);
        // orderId must be null for advance payments (no standard order involved)
        assertThat(response.getOrderId()).isNull();
    }

    // =========================================================================
    // ADV-S-04: initiateAdvancePayment — request not APPROVED → 409
    // =========================================================================

    @Test
    @DisplayName("ADV-S-04: initiateAdvancePayment before approval (request IN_PRODUCTION) → InvalidWorkflowTransitionException")
    void initiateAdvancePayment_beforeApproval_throwsException() {
        AppUser customer = buildCustomer(1L);
        // Request already in-production (advance payment not allowed again)
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.IN_PRODUCTION);

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> advancePaymentService.initiateAdvancePayment(customer, 10L, payReq))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("APPROVED");
    }

    // =========================================================================
    // ADV-S-05: initiateAdvancePayment — duplicate successful payment → 409
    // =========================================================================

    @Test
    @DisplayName("ADV-S-05: initiateAdvancePayment with existing successful payment → InvalidWorkflowTransitionException")
    void initiateAdvancePayment_duplicateSuccess_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.APPROVED);
        Payment existingSuccess = buildPayment(200L, req, PaymentPurpose.ADVANCE, PaymentStatus.SUCCESS);

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of(existingSuccess));

        assertThatThrownBy(() -> advancePaymentService.initiateAdvancePayment(customer, 10L, payReq))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("already completed");
    }

    // =========================================================================
    // ADV-S-06: initiateAdvancePayment — foreign request → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("ADV-S-06: initiateAdvancePayment for foreign customer's request → 404 (non-disclosure)")
    void initiateAdvancePayment_foreignRequest_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.APPROVED);

        PaymentInitiationRequest payReq = new PaymentInitiationRequest();
        payReq.setPaymentMethod("CARD");

        when(requestRepository.findById(20L)).thenReturn(Optional.of(foreignReq));

        assertThatThrownBy(() -> advancePaymentService.initiateAdvancePayment(customer, 20L, payReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // ADV-S-07: getCustomRequestPayments — returns payments for owned request
    // =========================================================================

    @Test
    @DisplayName("ADV-S-07: getCustomRequestPayments returns payments for owned request")
    void getCustomRequestPayments_returnsPayments() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.IN_PRODUCTION);
        Payment p = buildPayment(200L, req, PaymentPurpose.ADVANCE, PaymentStatus.SUCCESS);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(paymentRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of(p));

        List<PaymentResponse> result = advancePaymentService.getCustomRequestPayments(customer, 10L);

        assertThat(result).hasSize(1);
    }

    // =========================================================================
    // ADV-S-08: getCustomRequestPayments — foreign request → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("ADV-S-08: getCustomRequestPayments for foreign request → 404 (non-disclosure)")
    void getCustomRequestPayments_foreignRequest_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.IN_PRODUCTION);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(foreignReq));

        assertThatThrownBy(() -> advancePaymentService.getCustomRequestPayments(customer, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // ADV-S-09: getCustomRequestShipment — returns shipment for owned request
    // =========================================================================

    @Test
    @DisplayName("ADV-S-09: getCustomRequestShipment returns shipment for owned request")
    void getCustomRequestShipment_returnsShipment() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.SHIPPED);
        Shipment shipment = buildShipment(300L, req, ShipmentStatus.SHIPPED);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(shipmentRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(shipment));

        ShipmentResponse response = advancePaymentService.getCustomRequestShipment(customer, 10L);

        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
    }

    // =========================================================================
    // ADV-S-10: getCustomRequestShipment — no shipment → 404
    // =========================================================================

    @Test
    @DisplayName("ADV-S-10: getCustomRequestShipment when no shipment exists → ResourceNotFoundException")
    void getCustomRequestShipment_noShipment_returns404() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.IN_PRODUCTION);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(shipmentRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> advancePaymentService.getCustomRequestShipment(customer, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shipment not found");
    }

    // =========================================================================
    // ADV-S-11: getCustomRequestShipment — cross-customer request → 404
    // =========================================================================

    @Test
    @DisplayName("ADV-S-11: getCustomRequestShipment for cross-customer request → 404 (non-disclosure)")
    void getCustomRequestShipment_crossCustomer_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.SHIPPED);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(foreignReq));

        assertThatThrownBy(() -> advancePaymentService.getCustomRequestShipment(customer, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // PROD-S-01: updateCustomRequestStatus — IN_PRODUCTION → COMPLETED (valid)
    // =========================================================================

    @Test
    @DisplayName("PROD-S-01: updateCustomRequestStatus IN_PRODUCTION → COMPLETED succeeds")
    void updateCustomRequestStatus_inProductionToCompleted_succeeds() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.IN_PRODUCTION);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response =
                adminProductionService.updateCustomRequestStatus(10L, CustomOrderRequestStatus.COMPLETED);

        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.COMPLETED);
    }

    // =========================================================================
    // PROD-S-02: updateCustomRequestStatus — COMPLETED → SHIPPED (valid)
    // =========================================================================

    @Test
    @DisplayName("PROD-S-02: updateCustomRequestStatus COMPLETED → SHIPPED succeeds")
    void updateCustomRequestStatus_completedToShipped_succeeds() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.COMPLETED);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response =
                adminProductionService.updateCustomRequestStatus(10L, CustomOrderRequestStatus.SHIPPED);

        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.SHIPPED);
    }

    // =========================================================================
    // PROD-S-03: updateCustomRequestStatus — invalid transition → 409
    // =========================================================================

    @Test
    @DisplayName("PROD-S-03: updateCustomRequestStatus with invalid transition → InvalidWorkflowTransitionException")
    void updateCustomRequestStatus_invalidTransition_throwsException() {
        AppUser customer = buildCustomer(1L);
        // REQUESTED → DELIVERED is not a valid admin production transition
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() ->
                adminProductionService.updateCustomRequestStatus(10L, CustomOrderRequestStatus.DELIVERED))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
    }

    // =========================================================================
    // PROD-S-04: createShipment — creates shipment for custom request with PENDING status
    // =========================================================================

    @Test
    @DisplayName("PROD-S-04: createShipment creates a PENDING shipment for a custom request")
    void createShipment_forCustomRequest_createsPendingShipment() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.COMPLETED);

        com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest createReq =
                new com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest();
        createReq.setCustomOrderRequestId(10L);
        createReq.setCarrierName("TestCarrier");
        createReq.setTrackingReference("TRACK123");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(shipmentRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            setId(s, Shipment.class, 300L);
            return s;
        });

        ShipmentResponse response = adminProductionService.createShipment(createReq);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(response.getCarrierName()).isEqualTo("TestCarrier");
    }

    // =========================================================================
    // PROD-S-05: createShipment — duplicate shipment → 409
    // =========================================================================

    @Test
    @DisplayName("PROD-S-05: createShipment when shipment already exists for request → InvalidWorkflowTransitionException")
    void createShipment_duplicate_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.COMPLETED);
        Shipment existingShipment = buildShipment(300L, req, ShipmentStatus.PENDING);

        com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest createReq =
                new com.handmadeart.ecommerce.dto.customartwork.ShipmentCreateRequest();
        createReq.setCustomOrderRequestId(10L);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(shipmentRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(existingShipment));

        assertThatThrownBy(() -> adminProductionService.createShipment(createReq))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("already exists");
    }

    // =========================================================================
    // PROD-S-06: updateShipmentStatus — PENDING → SHIPPED + custom request advanced
    // =========================================================================

    @Test
    @DisplayName("PROD-S-06: updateShipmentStatus PENDING → SHIPPED sets shippedAt and advances custom request")
    void updateShipmentStatus_pendingToShipped_setsShippedAtAndAdvancesRequest() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.COMPLETED);
        Shipment shipment = buildShipment(300L, req, ShipmentStatus.PENDING);

        ShipmentStatusUpdateRequest statusReq = new ShipmentStatusUpdateRequest();
        statusReq.setStatus(ShipmentStatus.SHIPPED);

        when(shipmentRepository.findById(300L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ShipmentResponse response = adminProductionService.updateShipmentStatus(300L, statusReq);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(response.getShippedAt()).isNotNull();
        verify(requestRepository).save(any(CustomOrderRequest.class));
    }

    // =========================================================================
    // PROD-S-07: updateShipmentStatus — SHIPPED → DELIVERED + custom request advanced
    // =========================================================================

    @Test
    @DisplayName("PROD-S-07: updateShipmentStatus SHIPPED → DELIVERED sets deliveredAt and advances custom request")
    void updateShipmentStatus_shippedToDelivered_setsDeliveredAtAndAdvancesRequest() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.SHIPPED);
        Shipment shipment = buildShipment(300L, req, ShipmentStatus.SHIPPED);

        ShipmentStatusUpdateRequest statusReq = new ShipmentStatusUpdateRequest();
        statusReq.setStatus(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findById(300L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(CustomOrderRequestStatus.DELIVERED);
            return saved;
        });

        ShipmentResponse response = adminProductionService.updateShipmentStatus(300L, statusReq);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(response.getDeliveredAt()).isNotNull();
    }

    // =========================================================================
    // PROD-S-08: updateShipmentStatus — invalid transition → 409
    // =========================================================================

    @Test
    @DisplayName("PROD-S-08: updateShipmentStatus with invalid transition (DELIVERED → SHIPPED) → InvalidWorkflowTransitionException")
    void updateShipmentStatus_invalidTransition_throwsException() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.DELIVERED);
        Shipment shipment = buildShipment(300L, req, ShipmentStatus.DELIVERED);

        ShipmentStatusUpdateRequest statusReq = new ShipmentStatusUpdateRequest();
        statusReq.setStatus(ShipmentStatus.SHIPPED); // DELIVERED is terminal

        when(shipmentRepository.findById(300L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> adminProductionService.updateShipmentStatus(300L, statusReq))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
    }
}
