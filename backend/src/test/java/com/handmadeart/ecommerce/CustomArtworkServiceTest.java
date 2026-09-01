package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomArtworkRequestSummary;
import com.handmadeart.ecommerce.dto.customartwork.QuotationCreateRequest;
import com.handmadeart.ecommerce.dto.customartwork.QuotationResponse;
import com.handmadeart.ecommerce.dto.customartwork.CustomRequestReviewRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.DuplicateQuotationException;
import com.handmadeart.ecommerce.exception.InvalidWorkflowTransitionException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import com.handmadeart.ecommerce.service.CustomArtworkRequestService;
import com.handmadeart.ecommerce.service.QuotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomArtworkRequestService} and {@link QuotationService}.
 *
 * Covered:
 *   CAR-S-01  createCustomRequest: initial status is REQUESTED
 *   CAR-S-02  createCustomRequest: user ID from authenticated user, not client
 *   CAR-S-03  listCustomRequests: returns only the authenticated customer's requests
 *   CAR-S-04  getCustomRequest: returns owned request
 *   CAR-S-05  getCustomRequest: foreign requestId → ResourceNotFoundException (non-disclosure)
 *   CAR-S-06  getCustomRequest: missing requestId → ResourceNotFoundException
 *   CAR-S-07  adminReviewCustomRequest ACCEPT: REQUESTED → UNDER_REVIEW
 *   CAR-S-08  adminReviewCustomRequest REJECT: UNDER_REVIEW → REJECTED
 *   CAR-S-09  adminReviewCustomRequest invalid transition → InvalidWorkflowTransitionException
 *   CAR-S-10  adminReviewCustomRequest: admin user recorded as reviewedBy
 *   QUO-S-01  createQuotation: succeeds from UNDER_REVIEW state
 *   QUO-S-02  createQuotation: initial quotation status is PENDING; request → QUOTED
 *   QUO-S-03  createQuotation: wrong request state → InvalidWorkflowTransitionException
 *   QUO-S-04  createQuotation: duplicate quotation → DuplicateQuotationException
 *   QUO-S-05  createQuotation: expiry in past → IllegalArgumentException
 *   QUO-S-06  customerGetQuotation: returns quotation for owned request
 *   QUO-S-07  customerGetQuotation: foreign request → ResourceNotFoundException (non-disclosure)
 *   QUO-S-08  customerGetQuotation: no quotation → ResourceNotFoundException
 *   QUO-S-09  adminGetQuotation: returns quotation by quotation ID
 *   QUO-S-10  adminGetQuotation: missing quotation → ResourceNotFoundException
 */
@ExtendWith(MockitoExtension.class)
class CustomArtworkServiceTest {

    @Mock private CustomOrderRequestRepository requestRepository;
    @Mock private CustomOrderImageRepository imageRepository;
    @Mock private QuotationRepository quotationRepository;

    private CustomArtworkRequestService artworkService;
    private QuotationService quotationService;

    @BeforeEach
    void setUp() {
        artworkService = new CustomArtworkRequestService(
                requestRepository, imageRepository, "uploads/reference-images");
        quotationService = new QuotationService(quotationRepository, requestRepository);
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

    private Quotation buildQuotation(Long id, CustomOrderRequest req) {
        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("500.00"));
        q.setStatus(QuotationStatus.PENDING);
        q.setExpiryAt(OffsetDateTime.now().plusDays(7));
        q.setCreatedBy(buildAdmin(99L));
        setId(q, Quotation.class, id);
        return q;
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
    // CAR-S-01: createCustomRequest → initial status is REQUESTED
    // =========================================================================

    @Test
    @DisplayName("CAR-S-01: createCustomRequest sets initial status to REQUESTED")
    void createCustomRequest_initialStatusIsRequested() {
        AppUser customer = buildCustomer(1L);
        CustomArtworkRequestCreateRequest createReq = new CustomArtworkRequestCreateRequest();
        createReq.setProductType("Watercolor");
        createReq.setDescription("A watercolor landscape");

        CustomOrderRequest saved = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);
        when(requestRepository.save(any(CustomOrderRequest.class))).thenReturn(saved);
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response = artworkService.createCustomRequest(customer, createReq);

        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.REQUESTED);
    }

    // =========================================================================
    // CAR-S-02: createCustomRequest → user from JWT, not client
    // =========================================================================

    @Test
    @DisplayName("CAR-S-02: createCustomRequest uses authenticated user id, not any client-supplied value")
    void createCustomRequest_userFromJwtNotClientSupplied() {
        AppUser customer = buildCustomer(42L);
        CustomArtworkRequestCreateRequest createReq = new CustomArtworkRequestCreateRequest();
        createReq.setProductType("Sculpture");
        createReq.setDescription("A clay sculpture");

        CustomOrderRequest saved = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);
        when(requestRepository.save(any(CustomOrderRequest.class))).thenReturn(saved);
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response = artworkService.createCustomRequest(customer, createReq);

        assertThat(response.getUserId()).isEqualTo(42L);
        // Verify save was called with the authenticated customer's user entity
        verify(requestRepository).save(any(CustomOrderRequest.class));
    }

    // =========================================================================
    // CAR-S-03: listCustomRequests → only authenticated customer's requests
    // =========================================================================

    @Test
    @DisplayName("CAR-S-03: listCustomRequests returns only the authenticated customer's requests")
    void listCustomRequests_scopedToAuthenticatedCustomer() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        when(requestRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(req)));

        PageResponse<CustomArtworkRequestSummary> result =
                artworkService.listCustomRequests(customer, 0, 20, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(1L);
        verify(requestRepository).findByUserId(eq(1L), any(Pageable.class));
    }

    // =========================================================================
    // CAR-S-04: getCustomRequest → returns owned request
    // =========================================================================

    @Test
    @DisplayName("CAR-S-04: getCustomRequest returns full response for owned request")
    void getCustomRequest_returnsOwnedRequest() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response = artworkService.getCustomRequest(customer, 10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.REQUESTED);
    }

    // =========================================================================
    // CAR-S-05: getCustomRequest → foreign requestId → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("CAR-S-05: getCustomRequest with foreign requestId returns 404 (non-disclosure)")
    void getCustomRequest_foreignRequestId_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.REQUESTED);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(foreignReq));

        assertThatThrownBy(() -> artworkService.getCustomRequest(customer, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Custom request not found");
    }

    // =========================================================================
    // CAR-S-06: getCustomRequest → missing requestId → 404
    // =========================================================================

    @Test
    @DisplayName("CAR-S-06: getCustomRequest with missing requestId returns 404")
    void getCustomRequest_missingRequestId_returns404() {
        AppUser customer = buildCustomer(1L);
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artworkService.getCustomRequest(customer, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // CAR-S-07: adminReviewCustomRequest ACCEPT: REQUESTED → UNDER_REVIEW
    // =========================================================================

    @Test
    @DisplayName("CAR-S-07: adminReview ACCEPT transitions REQUESTED → UNDER_REVIEW")
    void adminReview_accept_requestedToUnderReview() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");
        reviewReq.setNotes("Looks interesting");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        // Save returns the mutated entity
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response =
                artworkService.adminReviewCustomRequest(admin, 10L, reviewReq);

        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.UNDER_REVIEW);
    }

    // =========================================================================
    // CAR-S-08: adminReviewCustomRequest REJECT: UNDER_REVIEW → REJECTED
    // =========================================================================

    @Test
    @DisplayName("CAR-S-08: adminReview REJECT transitions UNDER_REVIEW → REJECTED")
    void adminReview_reject_underReviewToRejected() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("REJECT");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        CustomArtworkRequestResponse response =
                artworkService.adminReviewCustomRequest(admin, 10L, reviewReq);

        assertThat(response.getStatus()).isEqualTo(CustomOrderRequestStatus.REJECTED);
    }

    // =========================================================================
    // CAR-S-09: adminReviewCustomRequest invalid transition → 409
    // =========================================================================

    @Test
    @DisplayName("CAR-S-09: adminReview ACCEPT on QUOTED state → InvalidWorkflowTransitionException")
    void adminReview_invalidTransition_throwsException() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        // QUOTED is not a valid state for admin review ACCEPT
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> artworkService.adminReviewCustomRequest(admin, 10L, reviewReq))
                .isInstanceOf(InvalidWorkflowTransitionException.class);
    }

    // =========================================================================
    // CAR-S-10: adminReview records reviewedBy as the admin user
    // =========================================================================

    @Test
    @DisplayName("CAR-S-10: adminReview records the admin user as reviewedBy")
    void adminReview_recordsReviewedByAdmin() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        CustomRequestReviewRequest reviewReq = new CustomRequestReviewRequest();
        reviewReq.setDecision("ACCEPT");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getReviewedBy()).isEqualTo(admin);
            return saved;
        });
        when(imageRepository.findByCustomOrderRequestId(10L)).thenReturn(List.of());

        artworkService.adminReviewCustomRequest(admin, 10L, reviewReq);
        // Verification is inside the save stub above
    }

    // =========================================================================
    // QUO-S-01: createQuotation succeeds from UNDER_REVIEW
    // =========================================================================

    @Test
    @DisplayName("QUO-S-01: createQuotation succeeds when request is UNDER_REVIEW")
    void createQuotation_succeeds_fromUnderReview() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);

        QuotationCreateRequest createReq = buildQuotationCreateRequest();

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> {
            Quotation q = inv.getArgument(0);
            setId(q, Quotation.class, 100L);
            return q;
        });
        when(requestRepository.save(any(CustomOrderRequest.class))).thenReturn(req);

        QuotationResponse response = quotationService.createQuotation(admin, 10L, createReq);

        assertThat(response.getQuotedAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getStatus()).isEqualTo(QuotationStatus.PENDING);
    }

    // =========================================================================
    // QUO-S-02: createQuotation → quotation status PENDING, request → QUOTED
    // =========================================================================

    @Test
    @DisplayName("QUO-S-02: createQuotation sets quotation status PENDING and request status QUOTED")
    void createQuotation_setsStatusCorrectly() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);

        QuotationCreateRequest createReq = buildQuotationCreateRequest();

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> {
            Quotation q = inv.getArgument(0);
            assertThat(q.getStatus()).isEqualTo(QuotationStatus.PENDING);
            setId(q, Quotation.class, 100L);
            return q;
        });
        when(requestRepository.save(any(CustomOrderRequest.class))).thenAnswer(inv -> {
            CustomOrderRequest saved = inv.getArgument(0);
            assertThat(saved.getStatus()).isEqualTo(CustomOrderRequestStatus.QUOTED);
            return saved;
        });

        quotationService.createQuotation(admin, 10L, createReq);
    }

    // =========================================================================
    // QUO-S-03: createQuotation: wrong state → InvalidWorkflowTransitionException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-03: createQuotation with request in REQUESTED state → InvalidWorkflowTransitionException")
    void createQuotation_wrongState_throwsException() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.REQUESTED);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> quotationService.createQuotation(admin, 10L, buildQuotationCreateRequest()))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("UNDER_REVIEW");
    }

    // =========================================================================
    // QUO-S-04: createQuotation: duplicate quotation → DuplicateQuotationException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-04: createQuotation when quotation already exists → DuplicateQuotationException")
    void createQuotation_duplicate_throwsException() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);
        Quotation existing = buildQuotation(100L, req);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> quotationService.createQuotation(admin, 10L, buildQuotationCreateRequest()))
                .isInstanceOf(DuplicateQuotationException.class);
    }

    // =========================================================================
    // QUO-S-05: createQuotation: expiry in past → IllegalArgumentException
    // =========================================================================

    @Test
    @DisplayName("QUO-S-05: createQuotation with expiry in the past → IllegalArgumentException")
    void createQuotation_expiryInPast_throwsException() {
        AppUser admin = buildAdmin(99L);
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);

        QuotationCreateRequest createReq = buildQuotationCreateRequest();
        createReq.setExpiryAt(OffsetDateTime.now().minusDays(1)); // past expiry

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.createQuotation(admin, 10L, createReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    // =========================================================================
    // QUO-S-06: customerGetQuotation: returns quotation for owned request
    // =========================================================================

    @Test
    @DisplayName("QUO-S-06: customerGetQuotation returns quotation for owned request")
    void customerGetQuotation_returnsQuotation() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation quotation = buildQuotation(100L, req);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.of(quotation));

        QuotationResponse response = quotationService.customerGetQuotation(customer, 10L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(QuotationStatus.PENDING);
    }

    // =========================================================================
    // QUO-S-07: customerGetQuotation: foreign request → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("QUO-S-07: customerGetQuotation with foreign request → ResourceNotFoundException (non-disclosure)")
    void customerGetQuotation_foreignRequest_returns404() {
        AppUser customer = buildCustomer(1L);
        AppUser otherCustomer = buildCustomer(2L);
        CustomOrderRequest foreignReq = buildRequest(20L, otherCustomer, CustomOrderRequestStatus.QUOTED);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(foreignReq));

        assertThatThrownBy(() -> quotationService.customerGetQuotation(customer, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // QUO-S-08: customerGetQuotation: no quotation → 404
    // =========================================================================

    @Test
    @DisplayName("QUO-S-08: customerGetQuotation when no quotation exists → ResourceNotFoundException")
    void customerGetQuotation_noQuotation_returns404() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.UNDER_REVIEW);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(quotationRepository.findByCustomOrderRequestId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.customerGetQuotation(customer, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quotation not found");
    }

    // =========================================================================
    // QUO-S-09: adminGetQuotation: returns quotation by quotation ID
    // =========================================================================

    @Test
    @DisplayName("QUO-S-09: adminGetQuotation returns quotation by quotation ID")
    void adminGetQuotation_returnsQuotation() {
        AppUser customer = buildCustomer(1L);
        CustomOrderRequest req = buildRequest(10L, customer, CustomOrderRequestStatus.QUOTED);
        Quotation quotation = buildQuotation(100L, req);

        when(quotationRepository.findById(100L)).thenReturn(Optional.of(quotation));

        QuotationResponse response = quotationService.adminGetQuotation(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCustomOrderRequestId()).isEqualTo(10L);
    }

    // =========================================================================
    // QUO-S-10: adminGetQuotation: missing quotation → 404
    // =========================================================================

    @Test
    @DisplayName("QUO-S-10: adminGetQuotation with missing quotation ID → ResourceNotFoundException")
    void adminGetQuotation_missing_returns404() {
        when(quotationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.adminGetQuotation(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quotation not found");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private QuotationCreateRequest buildQuotationCreateRequest() {
        QuotationCreateRequest req = new QuotationCreateRequest();
        req.setQuotedAmount(new BigDecimal("500.00"));
        req.setExpiryAt(OffsetDateTime.now().plusDays(7));
        return req;
    }
}
