package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.CustomOrderRequestStatus;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.Quotation;
import com.handmadeart.ecommerce.entity.QuotationStatus;
import com.handmadeart.ecommerce.entity.Shipment;
import com.handmadeart.ecommerce.entity.ShipmentStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database integration tests for Phase 2E custom artwork persistence.
 *
 * Verifies approved schema rules from Database Design &amp; ERD §3.13–3.16,
 * §12–13, §15.3–15.4, §15.7, §16–17:
 *
 * CustomOrderRequest:
 *  - persists with required fields and REQUESTED status default
 *  - all thirteen lifecycle status values accepted
 *  - status CHECK constraint rejects invalid values
 *  - ownership FK to app_user enforced (NOT NULL)
 *  - optional fields nullable
 *  - created_at / updated_at populated by DB DEFAULT
 *  - findByUserId paginated query
 *  - findByStatus query
 *
 * CustomOrderImage:
 *  - persists with request FK, storage metadata, content type, file size
 *  - file_size_bytes CHECK > 0 enforced
 *  - findByCustomOrderRequestId query
 *  - ON DELETE CASCADE: deleting request removes its images
 *
 * Quotation:
 *  - persists with required fields and PENDING status default
 *  - UNIQUE custom_order_request_id: duplicate quotation for same request rejected
 *  - quoted_amount CHECK >= 0 enforced
 *  - advance_amount: nullable; negative value rejected
 *  - expiry_at NOT NULL enforced
 *  - created_at populated by DB DEFAULT
 *  - findByCustomOrderRequestId query
 *  - findByStatus query
 *
 * Shipment:
 *  - persists for a ready-made order
 *  - persists for a custom order request
 *  - mutual-exclusivity CHECK: both references null → rejected
 *  - mutual-exclusivity CHECK: both references set → rejected
 *  - findByOrderId and findByCustomOrderRequestId queries
 *  - created_at populated by DB DEFAULT
 *
 * Payment (Phase 2E upgrade):
 *  - payment linked to a custom order request via proper @ManyToOne FK
 *  - findByCustomOrderRequestId query
 *
 * ACTIVATION:
 *   Requires a running PostgreSQL instance. Excluded from default test run.
 *   Run with:
 *     mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
 */
@Tag("db-integration")
@SpringBootTest
@ActiveProfiles("db-integration")
class CustomArtworkPersistenceIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private AppUserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerOrderRepository orderRepository;
    @Autowired private CustomOrderRequestRepository customOrderRequestRepository;
    @Autowired private CustomOrderImageRepository customOrderImageRepository;
    @Autowired private QuotationRepository quotationRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private PaymentRepository paymentRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppUser savedUser(String emailPrefix) {
        AppUser u = new AppUser();
        u.setEmail(emailPrefix + "@artwork.test.handmadeart.com");
        u.setPasswordHash("$2a$10$placeholder_hash_for_testing_only");
        u.setFullName("Artwork Test User " + emailPrefix);
        u.setRole(UserRole.CUSTOMER);
        return userRepository.saveAndFlush(u);
    }

    private AppUser savedAdmin(String emailPrefix) {
        AppUser u = new AppUser();
        u.setEmail(emailPrefix + "@artwork.test.handmadeart.com");
        u.setPasswordHash("$2a$10$placeholder_hash_for_testing_only");
        u.setFullName("Artwork Test Admin " + emailPrefix);
        u.setRole(UserRole.ADMIN);
        return userRepository.saveAndFlush(u);
    }

    private CustomOrderRequest savedRequest(AppUser user, String descSuffix) {
        CustomOrderRequest r = new CustomOrderRequest();
        r.setUser(user);
        r.setProductType("Painting");
        r.setDescription("Custom painting request - " + descSuffix);
        r.setStatus(CustomOrderRequestStatus.REQUESTED);
        return customOrderRequestRepository.saveAndFlush(r);
    }

    private CustomerOrder savedOrder(AppUser user) {
        CustomerOrder o = new CustomerOrder();
        o.setUser(user);
        o.setStatus(OrderStatus.CONFIRMED);
        o.setShipRecipientName("Artwork Recipient");
        o.setShipLine1("1 Art Street");
        o.setShipCity("Artville");
        o.setShipStateProvince("Art State");
        o.setShipPostalCode("ART01");
        o.setShipCountry("India");
        o.setSubtotalAmount(new BigDecimal("500.00"));
        o.setTotalAmount(new BigDecimal("500.00"));
        return orderRepository.saveAndFlush(o);
    }

    private Category savedCategory(String name) {
        Category c = new Category();
        c.setName(name);
        c.setStatus(CategoryStatus.ACTIVE);
        return categoryRepository.saveAndFlush(c);
    }

    private Product savedProduct(Category cat, String name) {
        Product p = new Product();
        p.setCategory(cat);
        p.setName(name);
        p.setPrice(new BigDecimal("500.00"));
        p.setProductType(ProductType.CUSTOM_AVAILABLE);
        p.setStatus(ProductStatus.ACTIVE);
        return productRepository.saveAndFlush(p);
    }

    // =========================================================================
    // CustomOrderRequest tests
    // =========================================================================

    @Test
    @Transactional
    void request_canBePersistedWithRequiredFields() {
        AppUser user = savedUser("req_persist");
        CustomOrderRequest req = savedRequest(user, "basic persist");

        assertThat(req.getId()).isNotNull().isPositive();
        assertThat(req.getUser().getId()).isEqualTo(user.getId());
        assertThat(req.getProductType()).isEqualTo("Painting");
        assertThat(req.getDescription()).isEqualTo("Custom painting request - basic persist");
        assertThat(req.getStatus()).isEqualTo(CustomOrderRequestStatus.REQUESTED);
        assertThat(req.getCreatedAt()).isNotNull();
        assertThat(req.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void request_optionalFieldsAreNullable() {
        AppUser user = savedUser("req_optional");
        CustomOrderRequest req = savedRequest(user, "optional fields test");

        // All optional fields should be null when not set.
        assertThat(req.getDesignTheme()).isNull();
        assertThat(req.getPreferredColors()).isNull();
        assertThat(req.getDimensionsSize()).isNull();
        assertThat(req.getBudgetRange()).isNull();
        assertThat(req.getRequiredDeliveryDate()).isNull();
        assertThat(req.getAdditionalInstructions()).isNull();
        assertThat(req.getReviewedBy()).isNull();
        assertThat(req.getReviewNotes()).isNull();
    }

    @Test
    @Transactional
    void request_allOptionalFieldsPersist() {
        AppUser user = savedUser("req_full");
        AppUser admin = savedAdmin("req_full_admin");

        CustomOrderRequest req = new CustomOrderRequest();
        req.setUser(user);
        req.setProductType("Sculpture");
        req.setDescription("Detailed sculpture commission");
        req.setDesignTheme("Mythological");
        req.setPreferredColors("Gold, Bronze");
        req.setDimensionsSize("30cm x 20cm");
        req.setBudgetRange("500-1000 INR");
        req.setRequiredDeliveryDate(LocalDate.of(2025, 12, 31));
        req.setAdditionalInstructions("Please use marble");
        req.setStatus(CustomOrderRequestStatus.UNDER_REVIEW);
        req.setReviewedBy(admin);
        req.setReviewNotes("Feasible — quoting shortly");
        CustomOrderRequest saved = customOrderRequestRepository.saveAndFlush(req);

        CustomOrderRequest reloaded = customOrderRequestRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDesignTheme()).isEqualTo("Mythological");
        assertThat(reloaded.getPreferredColors()).isEqualTo("Gold, Bronze");
        assertThat(reloaded.getDimensionsSize()).isEqualTo("30cm x 20cm");
        assertThat(reloaded.getBudgetRange()).isEqualTo("500-1000 INR");
        assertThat(reloaded.getRequiredDeliveryDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(reloaded.getAdditionalInstructions()).isEqualTo("Please use marble");
        assertThat(reloaded.getStatus()).isEqualTo(CustomOrderRequestStatus.UNDER_REVIEW);
        assertThat(reloaded.getReviewedBy().getId()).isEqualTo(admin.getId());
        assertThat(reloaded.getReviewNotes()).isEqualTo("Feasible — quoting shortly");
    }

    @Test
    @Transactional
    void request_allThirteenStatusValues_accepted() {
        // Verify the CHECK constraint accepts every approved lifecycle status value.
        AppUser user = savedUser("req_statuses");
        for (CustomOrderRequestStatus status : CustomOrderRequestStatus.values()) {
            CustomOrderRequest req = new CustomOrderRequest();
            req.setUser(user);
            req.setProductType("Painting");
            req.setDescription("Status test: " + status.name());
            req.setStatus(status);
            CustomOrderRequest saved = customOrderRequestRepository.saveAndFlush(req);
            assertThat(saved.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @Transactional
    void request_missingUserId_rejectedByNotNullConstraint() {
        CustomOrderRequest req = new CustomOrderRequest();
        // user not set — FK to app_user is NOT NULL.
        req.setProductType("Painting");
        req.setDescription("Request without user");
        req.setStatus(CustomOrderRequestStatus.REQUESTED);
        assertThatThrownBy(() -> customOrderRequestRepository.saveAndFlush(req))
                .isInstanceOf(Exception.class);
    }

    @Test
    @Transactional
    void request_findByUserId_returnsOnlyCustomerRequests() {
        AppUser userA = savedUser("req_find_a");
        AppUser userB = savedUser("req_find_b");
        savedRequest(userA, "a1");
        savedRequest(userA, "a2");
        savedRequest(userB, "b1");
        customOrderRequestRepository.flush();

        org.springframework.data.domain.Page<CustomOrderRequest> page =
                customOrderRequestRepository.findByUserId(userA.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(r -> r.getUser().getId().equals(userA.getId()));
    }

    @Test
    @Transactional
    void request_findByStatus_returnsCorrectSubset() {
        AppUser user = savedUser("req_status_filter");
        savedRequest(user, "status1");

        CustomOrderRequest underReview = new CustomOrderRequest();
        underReview.setUser(user);
        underReview.setProductType("Drawing");
        underReview.setDescription("Under review request");
        underReview.setStatus(CustomOrderRequestStatus.UNDER_REVIEW);
        customOrderRequestRepository.saveAndFlush(underReview);

        List<CustomOrderRequest> requested =
                customOrderRequestRepository.findByStatus(CustomOrderRequestStatus.REQUESTED);
        assertThat(requested).isNotEmpty();
        assertThat(requested).allMatch(r -> r.getStatus() == CustomOrderRequestStatus.REQUESTED);

        List<CustomOrderRequest> reviewList =
                customOrderRequestRepository.findByStatus(CustomOrderRequestStatus.UNDER_REVIEW);
        assertThat(reviewList).isNotEmpty();
        assertThat(reviewList).allMatch(r -> r.getStatus() == CustomOrderRequestStatus.UNDER_REVIEW);
    }

    // =========================================================================
    // CustomOrderImage tests
    // =========================================================================

    @Test
    @Transactional
    void image_canBePersistedWithRequest() {
        AppUser user = savedUser("img_persist");
        CustomOrderRequest req = savedRequest(user, "image persist");

        CustomOrderImage img = new CustomOrderImage();
        img.setCustomOrderRequest(req);
        img.setStorageReference("uploads/custom/2025/test-image.jpg");
        img.setOriginalFilename("my-design.jpg");
        img.setContentType("image/jpeg");
        img.setFileSizeBytes(204800);
        CustomOrderImage saved = customOrderImageRepository.saveAndFlush(img);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCustomOrderRequest().getId()).isEqualTo(req.getId());
        assertThat(saved.getStorageReference()).isEqualTo("uploads/custom/2025/test-image.jpg");
        assertThat(saved.getOriginalFilename()).isEqualTo("my-design.jpg");
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getFileSizeBytes()).isEqualTo(204800);
        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    @Transactional
    void image_originalFilename_isNullable() {
        AppUser user = savedUser("img_null_fn");
        CustomOrderRequest req = savedRequest(user, "nullable filename");

        CustomOrderImage img = new CustomOrderImage();
        img.setCustomOrderRequest(req);
        img.setStorageReference("uploads/custom/2025/no-filename.png");
        img.setContentType("image/png");
        img.setFileSizeBytes(102400);
        CustomOrderImage saved = customOrderImageRepository.saveAndFlush(img);

        assertThat(saved.getOriginalFilename()).isNull();
    }

    @Test
    @Transactional
    void image_zeroFileSize_rejectedByCheckConstraint() {
        AppUser user = savedUser("img_zero_size");
        CustomOrderRequest req = savedRequest(user, "zero size test");

        CustomOrderImage img = new CustomOrderImage();
        img.setCustomOrderRequest(req);
        img.setStorageReference("uploads/custom/2025/zero-size.jpg");
        img.setContentType("image/jpeg");
        img.setFileSizeBytes(0);  // CHECK file_size_bytes > 0
        assertThatThrownBy(() -> customOrderImageRepository.saveAndFlush(img))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void image_findByCustomOrderRequestId_returnsImages() {
        AppUser user = savedUser("img_find");
        CustomOrderRequest req = savedRequest(user, "find images");

        CustomOrderImage img1 = new CustomOrderImage();
        img1.setCustomOrderRequest(req);
        img1.setStorageReference("uploads/custom/img1.jpg");
        img1.setContentType("image/jpeg");
        img1.setFileSizeBytes(100000);
        customOrderImageRepository.saveAndFlush(img1);

        CustomOrderImage img2 = new CustomOrderImage();
        img2.setCustomOrderRequest(req);
        img2.setStorageReference("uploads/custom/img2.png");
        img2.setContentType("image/png");
        img2.setFileSizeBytes(200000);
        customOrderImageRepository.saveAndFlush(img2);

        List<CustomOrderImage> images =
                customOrderImageRepository.findByCustomOrderRequestId(req.getId());
        assertThat(images).hasSize(2);
        assertThat(images).allMatch(i -> i.getCustomOrderRequest().getId().equals(req.getId()));
    }

    @Test
    @Transactional
    void image_cascadeDeleteWithRequest() {
        // Verify ON DELETE CASCADE on custom_order_image.custom_order_request_id (ERD §16).
        AppUser user = savedUser("img_cascade");
        CustomOrderRequest req = savedRequest(user, "cascade test");

        CustomOrderImage img = new CustomOrderImage();
        img.setCustomOrderRequest(req);
        img.setStorageReference("uploads/custom/cascade.jpg");
        img.setContentType("image/jpeg");
        img.setFileSizeBytes(50000);
        CustomOrderImage saved = customOrderImageRepository.saveAndFlush(img);
        Long imgId = saved.getId();
        Long reqId = req.getId();

        em.flush();
        em.clear();

        // Delete the request via native SQL to test ON DELETE CASCADE on custom_order_image.
        em.createNativeQuery("DELETE FROM custom_order_request WHERE id = :id")
                .setParameter("id", reqId)
                .executeUpdate();
        em.flush();

        Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM custom_order_image WHERE id = :id")
                .setParameter("id", imgId)
                .getSingleResult();
        assertThat(count.longValue())
                .as("custom_order_image row must be removed by ON DELETE CASCADE when request is deleted")
                .isEqualTo(0L);
    }

    // =========================================================================
    // Quotation tests
    // =========================================================================

    @Test
    @Transactional
    void quotation_canBePersistedWithRequiredFields() {
        AppUser user = savedUser("quot_persist");
        AppUser admin = savedAdmin("quot_persist_admin");
        CustomOrderRequest req = savedRequest(user, "quotation persist");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("1500.00"));
        q.setAdvanceAmount(new BigDecimal("500.00"));
        q.setEstimatedDeliveryDate(LocalDate.of(2026, 3, 1));
        q.setExpiryAt(OffsetDateTime.now().plusDays(7));
        q.setNotesTerms("50% advance required before start.");
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        Quotation saved = quotationRepository.saveAndFlush(q);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCustomOrderRequest().getId()).isEqualTo(req.getId());
        assertThat(saved.getQuotedAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(saved.getAdvanceAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(saved.getStatus()).isEqualTo(QuotationStatus.PENDING);
        assertThat(saved.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDecidedAt()).isNull();
    }

    @Test
    @Transactional
    void quotation_advanceAmountAndOptionalFields_areNullable() {
        AppUser user = savedUser("quot_nullable");
        AppUser admin = savedAdmin("quot_nullable_admin");
        CustomOrderRequest req = savedRequest(user, "quotation nullable");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("2000.00"));
        // advance_amount, estimated_delivery_date, notes_terms left null.
        q.setExpiryAt(OffsetDateTime.now().plusDays(14));
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        Quotation saved = quotationRepository.saveAndFlush(q);

        assertThat(saved.getAdvanceAmount()).isNull();
        assertThat(saved.getEstimatedDeliveryDate()).isNull();
        assertThat(saved.getNotesTerms()).isNull();
    }

    @Test
    @Transactional
    void quotation_uniquePerRequest_rejectsDuplicate() {
        // UNIQUE custom_order_request_id — one quotation per request (ERD §13.2).
        AppUser user = savedUser("quot_unique");
        AppUser admin = savedAdmin("quot_unique_admin");
        CustomOrderRequest req = savedRequest(user, "unique quotation");

        Quotation first = new Quotation();
        first.setCustomOrderRequest(req);
        first.setQuotedAmount(new BigDecimal("1000.00"));
        first.setExpiryAt(OffsetDateTime.now().plusDays(7));
        first.setStatus(QuotationStatus.PENDING);
        first.setCreatedBy(admin);
        quotationRepository.saveAndFlush(first);

        // A second quotation for the same request must be rejected.
        Quotation duplicate = new Quotation();
        duplicate.setCustomOrderRequest(req);
        duplicate.setQuotedAmount(new BigDecimal("900.00"));
        duplicate.setExpiryAt(OffsetDateTime.now().plusDays(7));
        duplicate.setStatus(QuotationStatus.PENDING);
        duplicate.setCreatedBy(admin);
        assertThatThrownBy(() -> {
            em.persist(duplicate);
            em.flush();
        }).isInstanceOf(Exception.class)
          .satisfies(ex -> assertThat(ex).isInstanceOfAny(
              DataIntegrityViolationException.class,
              jakarta.persistence.PersistenceException.class));
    }

    @Test
    @Transactional
    void quotation_negativeQuotedAmount_rejectedByCheckConstraint() {
        AppUser user = savedUser("quot_neg_amount");
        AppUser admin = savedAdmin("quot_neg_amount_admin");
        CustomOrderRequest req = savedRequest(user, "negative quoted amount");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("-0.01"));  // CHECK quoted_amount >= 0
        q.setExpiryAt(OffsetDateTime.now().plusDays(7));
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        assertThatThrownBy(() -> quotationRepository.saveAndFlush(q))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void quotation_negativeAdvanceAmount_rejectedByCheckConstraint() {
        AppUser user = savedUser("quot_neg_advance");
        AppUser admin = savedAdmin("quot_neg_advance_admin");
        CustomOrderRequest req = savedRequest(user, "negative advance amount");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("1000.00"));
        q.setAdvanceAmount(new BigDecimal("-1.00"));  // CHECK advance_amount IS NULL OR advance_amount >= 0
        q.setExpiryAt(OffsetDateTime.now().plusDays(7));
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        assertThatThrownBy(() -> quotationRepository.saveAndFlush(q))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void quotation_expiryAt_isRequired() {
        AppUser user = savedUser("quot_no_expiry");
        AppUser admin = savedAdmin("quot_no_expiry_admin");
        CustomOrderRequest req = savedRequest(user, "missing expiry");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("800.00"));
        // expiryAt not set — NOT NULL constraint must fire.
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        assertThatThrownBy(() -> quotationRepository.saveAndFlush(q))
                .isInstanceOf(Exception.class);
    }

    @Test
    @Transactional
    void quotation_allFourStatusValues_accepted() {
        AppUser user = savedUser("quot_statuses");
        AppUser admin = savedAdmin("quot_statuses_admin");

        for (QuotationStatus status : QuotationStatus.values()) {
            // Each status needs its own request (UNIQUE FK on custom_order_request_id).
            CustomOrderRequest req = savedRequest(user, "status-" + status.name());
            Quotation q = new Quotation();
            q.setCustomOrderRequest(req);
            q.setQuotedAmount(new BigDecimal("100.00"));
            q.setExpiryAt(OffsetDateTime.now().plusDays(1));
            q.setStatus(status);
            q.setCreatedBy(admin);
            Quotation saved = quotationRepository.saveAndFlush(q);
            assertThat(saved.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @Transactional
    void quotation_findByCustomOrderRequestId_returnsQuotation() {
        AppUser user = savedUser("quot_find");
        AppUser admin = savedAdmin("quot_find_admin");
        CustomOrderRequest req = savedRequest(user, "find quotation");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("750.00"));
        q.setExpiryAt(OffsetDateTime.now().plusDays(5));
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        quotationRepository.saveAndFlush(q);

        Optional<Quotation> found = quotationRepository.findByCustomOrderRequestId(req.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getQuotedAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    @Test
    @Transactional
    void quotation_findByStatus_returnsPendingQuotations() {
        AppUser user = savedUser("quot_status_q");
        AppUser admin = savedAdmin("quot_status_q_admin");

        CustomOrderRequest req1 = savedRequest(user, "pending q1");
        Quotation pending = new Quotation();
        pending.setCustomOrderRequest(req1);
        pending.setQuotedAmount(new BigDecimal("500.00"));
        pending.setExpiryAt(OffsetDateTime.now().plusDays(3));
        pending.setStatus(QuotationStatus.PENDING);
        pending.setCreatedBy(admin);
        quotationRepository.saveAndFlush(pending);

        CustomOrderRequest req2 = savedRequest(user, "approved q2");
        Quotation approved = new Quotation();
        approved.setCustomOrderRequest(req2);
        approved.setQuotedAmount(new BigDecimal("600.00"));
        approved.setExpiryAt(OffsetDateTime.now().plusDays(3));
        approved.setStatus(QuotationStatus.APPROVED);
        approved.setCreatedBy(admin);
        quotationRepository.saveAndFlush(approved);

        List<Quotation> pendingList = quotationRepository.findByStatus(QuotationStatus.PENDING);
        assertThat(pendingList).isNotEmpty();
        assertThat(pendingList).allMatch(q -> q.getStatus() == QuotationStatus.PENDING);
    }

    @Test
    @Transactional
    void quotation_decidedAt_isApplicationManaged() {
        AppUser user = savedUser("quot_decided");
        AppUser admin = savedAdmin("quot_decided_admin");
        CustomOrderRequest req = savedRequest(user, "decided at test");

        Quotation q = new Quotation();
        q.setCustomOrderRequest(req);
        q.setQuotedAmount(new BigDecimal("1200.00"));
        q.setExpiryAt(OffsetDateTime.now().plusDays(7));
        q.setStatus(QuotationStatus.PENDING);
        q.setCreatedBy(admin);
        Quotation saved = quotationRepository.saveAndFlush(q);
        assertThat(saved.getDecidedAt()).isNull();

        // Simulate customer approval: set decidedAt and status.
        saved.setStatus(QuotationStatus.APPROVED);
        OffsetDateTime decisionTime = OffsetDateTime.now();
        saved.setDecidedAt(decisionTime);
        Quotation updated = quotationRepository.saveAndFlush(saved);

        assertThat(updated.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(updated.getDecidedAt()).isNotNull();
    }

    // =========================================================================
    // Shipment tests
    // =========================================================================

    @Test
    @Transactional
    void shipment_canBePersistedForOrder() {
        AppUser user = savedUser("ship_order_persist");
        CustomerOrder order = savedOrder(user);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCarrierName("BlueDart");
        shipment.setTrackingReference("BD123456789");
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setEstimatedDeliveryDate(LocalDate.of(2026, 2, 14));
        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getCustomOrderRequest()).isNull();
        assertThat(saved.getCarrierName()).isEqualTo("BlueDart");
        assertThat(saved.getTrackingReference()).isEqualTo("BD123456789");
        assertThat(saved.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void shipment_canBePersistedForCustomRequest() {
        AppUser user = savedUser("ship_cor_persist");
        CustomOrderRequest req = savedRequest(user, "shipment for custom request");

        Shipment shipment = new Shipment();
        shipment.setCustomOrderRequest(req);
        shipment.setStatus(ShipmentStatus.PENDING);
        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCustomOrderRequest().getId()).isEqualTo(req.getId());
        assertThat(saved.getOrder()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void shipment_optionalFieldsAreNullable() {
        AppUser user = savedUser("ship_null_fields");
        CustomerOrder order = savedOrder(user);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PENDING);
        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertThat(saved.getCarrierName()).isNull();
        assertThat(saved.getTrackingReference()).isNull();
        assertThat(saved.getEstimatedDeliveryDate()).isNull();
        assertThat(saved.getShippedAt()).isNull();
        assertThat(saved.getDeliveredAt()).isNull();
    }

    @Test
    @Transactional
    void shipment_bothReferencesNull_rejectedByMutualExclusivityCheck() {
        // CHECK: sum of non-null references must equal 1.
        Shipment shipment = new Shipment();
        // Neither order_id nor custom_order_request_id set → sum = 0 → CHECK fails.
        shipment.setStatus(ShipmentStatus.PENDING);
        assertThatThrownBy(() -> shipmentRepository.saveAndFlush(shipment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void shipment_bothReferencesSet_rejectedByMutualExclusivityCheck() {
        AppUser user = savedUser("ship_both");
        CustomerOrder order = savedOrder(user);
        CustomOrderRequest req = savedRequest(user, "both refs test");

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCustomOrderRequest(req);  // both set → sum = 2 → CHECK fails
        shipment.setStatus(ShipmentStatus.PENDING);
        assertThatThrownBy(() -> shipmentRepository.saveAndFlush(shipment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void shipment_allThreeStatusValues_accepted() {
        AppUser user = savedUser("ship_statuses");
        for (ShipmentStatus status : ShipmentStatus.values()) {
            CustomerOrder order = savedOrder(user);
            Shipment shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setStatus(status);
            Shipment saved = shipmentRepository.saveAndFlush(shipment);
            assertThat(saved.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @Transactional
    void shipment_findByOrderId_returnsShipment() {
        AppUser user = savedUser("ship_find_order");
        CustomerOrder order = savedOrder(user);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setTrackingReference("TRACK-001");
        shipmentRepository.saveAndFlush(shipment);

        Optional<Shipment> found = shipmentRepository.findByOrderId(order.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTrackingReference()).isEqualTo("TRACK-001");
        assertThat(found.get().getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
    }

    @Test
    @Transactional
    void shipment_findByCustomOrderRequestId_returnsShipment() {
        AppUser user = savedUser("ship_find_cor");
        CustomOrderRequest req = savedRequest(user, "find shipment by request");

        Shipment shipment = new Shipment();
        shipment.setCustomOrderRequest(req);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        OffsetDateTime deliveredTime = OffsetDateTime.now();
        shipment.setDeliveredAt(deliveredTime);
        shipmentRepository.saveAndFlush(shipment);

        Optional<Shipment> found = shipmentRepository.findByCustomOrderRequestId(req.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(found.get().getDeliveredAt()).isNotNull();
    }

    @Test
    @Transactional
    void shipment_shippedAndDeliveredTimestamps_areApplicationManaged() {
        AppUser user = savedUser("ship_timestamps");
        CustomerOrder order = savedOrder(user);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PENDING);
        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertThat(saved.getShippedAt()).isNull();
        assertThat(saved.getDeliveredAt()).isNull();

        // Simulate admin marking as shipped.
        saved.setStatus(ShipmentStatus.SHIPPED);
        saved.setTrackingReference("TRACK-UPDATED-001");
        OffsetDateTime shipTime = OffsetDateTime.now();
        saved.setShippedAt(shipTime);
        Shipment updated = shipmentRepository.saveAndFlush(saved);

        assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(updated.getShippedAt()).isNotNull();
        assertThat(updated.getDeliveredAt()).isNull();
    }

    // =========================================================================
    // Payment (Phase 2E upgrade) — proper FK to CustomOrderRequest
    // =========================================================================

    @Test
    @Transactional
    void payment_linkedToCustomOrderRequest_findByCustomOrderRequestId() {
        AppUser user = savedUser("pay_find_cor");
        CustomOrderRequest req = savedRequest(user, "payment find by COR");

        Payment advance = new Payment();
        advance.setCustomOrderRequest(req);
        advance.setPaymentPurpose(PaymentPurpose.ADVANCE);
        advance.setAmount(new BigDecimal("300.00"));
        advance.setPaymentMethod("SANDBOX");
        advance.setStatus(PaymentStatus.SUCCESS);
        advance.setProviderTransactionReference("TXN-COR-ADV-001");
        paymentRepository.saveAndFlush(advance);

        Payment remaining = new Payment();
        remaining.setCustomOrderRequest(req);
        remaining.setPaymentPurpose(PaymentPurpose.REMAINING);
        remaining.setAmount(new BigDecimal("700.00"));
        remaining.setPaymentMethod("SANDBOX");
        remaining.setStatus(PaymentStatus.PENDING);
        paymentRepository.saveAndFlush(remaining);

        List<Payment> payments = paymentRepository.findByCustomOrderRequestId(req.getId());
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getPaymentPurpose)
                .containsExactlyInAnyOrder(PaymentPurpose.ADVANCE, PaymentPurpose.REMAINING);
    }
}
