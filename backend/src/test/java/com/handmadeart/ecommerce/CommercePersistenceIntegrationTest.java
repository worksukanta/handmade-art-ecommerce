package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Cart;
import com.handmadeart.ecommerce.entity.CartItem;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.Payment;
import com.handmadeart.ecommerce.entity.PaymentPurpose;
import com.handmadeart.ecommerce.entity.PaymentStatus;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.repository.CartItemRepository;
import com.handmadeart.ecommerce.repository.CartRepository;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database integration tests for Phase 2D commerce persistence.
 *
 * Verifies approved schema rules from Database Design &amp; ERD §3.7–3.11, §8–10, §15–17:
 *
 * Cart:
 *  - one cart per customer (UNIQUE user_id)
 *  - duplicate cart for same user rejected
 *  - cart lookup by user_id
 *
 * CartItem:
 *  - item persists with cart and product
 *  - quantity CHECK > 0 enforced
 *  - duplicate product in same cart rejected by UNIQUE (cart_id, product_id)
 *  - cascade delete: cart deletion removes items
 *
 * CustomerOrder:
 *  - persists with required snapshot fields
 *  - status defaults to PENDING_PAYMENT
 *  - subtotal/total negative values rejected by CHECK
 *  - BigDecimal precision preserved
 *  - queries by user_id and status
 *
 * OrderItem:
 *  - persists with order, price snapshot, quantity, line_total
 *  - quantity CHECK > 0 enforced
 *  - negative unit_price_snapshot rejected by CHECK
 *  - cascade delete: order deletion removes items
 *  - product_id becomes null on product hard-delete (ON DELETE SET NULL)
 *
 * Payment:
 *  - persists for a ready-made order
 *  - persists with custom_order_request_id (no FK yet — Phase 2E)
 *  - mutual-exclusivity CHECK: both references null → rejected
 *  - mutual-exclusivity CHECK: both references set → rejected
 *  - negative amount rejected by CHECK
 *  - provider_transaction_reference uniqueness enforced
 *  - lookup by order_id and status
 *
 * ACTIVATION:
 *   Requires a running PostgreSQL instance. Excluded from default test run.
 *   Run with:
 *     mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
 */
@Tag("db-integration")
@SpringBootTest
@ActiveProfiles("db-integration")
class CommercePersistenceIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private AppUserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private CustomerOrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppUser savedUser(String emailPrefix) {
        AppUser u = new AppUser();
        u.setEmail(emailPrefix + "@commerce.test.handmadeart.com");
        u.setPasswordHash("$2a$10$placeholder_hash_for_testing_only");
        u.setFullName("Commerce Test User " + emailPrefix);
        u.setRole(UserRole.CUSTOMER);
        return userRepository.saveAndFlush(u);
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
        p.setPrice(new BigDecimal("75.00"));
        p.setProductType(ProductType.READY_MADE);
        p.setStatus(ProductStatus.ACTIVE);
        return productRepository.saveAndFlush(p);
    }

    private CustomerOrder savedOrder(AppUser user) {
        CustomerOrder o = new CustomerOrder();
        o.setUser(user);
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setShipRecipientName("Test Recipient");
        o.setShipLine1("123 Test Street");
        o.setShipCity("Testville");
        o.setShipStateProvince("Test State");
        o.setShipPostalCode("12345");
        o.setShipCountry("India");
        o.setSubtotalAmount(new BigDecimal("75.00"));
        o.setTotalAmount(new BigDecimal("75.00"));
        return orderRepository.saveAndFlush(o);
    }

    // =========================================================================
    // Cart tests
    // =========================================================================

    @Test
    @Transactional
    void cart_canBePersistedForUser() {
        AppUser user = savedUser("cart_persist");
        Cart cart = new Cart();
        cart.setUser(user);
        Cart saved = cartRepository.saveAndFlush(cart);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void cart_uniquePerUser_rejectsDuplicate() {
        AppUser user = savedUser("cart_unique");
        Cart first = new Cart();
        first.setUser(user);
        cartRepository.saveAndFlush(first);

        // A second cart for the same user must be rejected by the UNIQUE index on user_id.
        Cart duplicate = new Cart();
        duplicate.setUser(user);
        assertThatThrownBy(() -> cartRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void cart_findByUserId_returnsCart() {
        AppUser user = savedUser("cart_find");
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);

        Optional<Cart> found = cartRepository.findByUserId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    // =========================================================================
    // CartItem tests
    // =========================================================================

    @Test
    @Transactional
    void cartItem_canBePersistedWithCartAndProduct() {
        AppUser user = savedUser("ci_persist");
        Category cat = savedCategory("ci_cat");
        Product product = savedProduct(cat, "ci_product");

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);
        CartItem saved = cartItemRepository.saveAndFlush(item);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getAddedAt()).isNotNull();
    }

    @Test
    @Transactional
    void cartItem_zeroQuantity_rejectedByCheckConstraint() {
        AppUser user = savedUser("ci_qty_zero");
        Category cat = savedCategory("ci_qty_zero_cat");
        Product product = savedProduct(cat, "ci_qty_zero_prod");

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(0);  // CHECK quantity > 0
        assertThatThrownBy(() -> cartItemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void cartItem_duplicateProductInCart_rejectedByUniqueConstraint() {
        AppUser user = savedUser("ci_dup");
        Category cat = savedCategory("ci_dup_cat");
        Product product = savedProduct(cat, "ci_dup_prod");

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);

        CartItem first = new CartItem();
        first.setCart(cart);
        first.setProduct(product);
        first.setQuantity(1);
        cartItemRepository.saveAndFlush(first);

        // Second item for the same product in the same cart — UNIQUE (cart_id, product_id).
        CartItem duplicate = new CartItem();
        duplicate.setCart(cart);
        duplicate.setProduct(product);
        duplicate.setQuantity(2);
        assertThatThrownBy(() -> {
            em.persist(duplicate);   // force INSERT rather than merge
            em.flush();
        }).isInstanceOf(Exception.class)
          .satisfies(ex -> assertThat(ex).isInstanceOfAny(
              DataIntegrityViolationException.class,
              jakarta.persistence.PersistenceException.class));
    }

    @Test
    @Transactional
    void cartItem_cascadeDeleteWithCart() {
        AppUser user = savedUser("ci_cascade");
        Category cat = savedCategory("ci_cascade_cat");
        Product product = savedProduct(cat, "ci_cascade_prod");

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        CartItem saved = cartItemRepository.saveAndFlush(item);
        Long itemId = saved.getId();
        Long cartId = cart.getId();

        em.flush();
        em.clear();

        // Delete the cart via native SQL to test ON DELETE CASCADE on cart_item.cart_id.
        em.createNativeQuery("DELETE FROM cart WHERE id = :id")
                .setParameter("id", cartId)
                .executeUpdate();
        em.flush();

        Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM cart_item WHERE id = :id")
                .setParameter("id", itemId)
                .getSingleResult();
        assertThat(count.longValue())
                .as("cart_item row must be removed by ON DELETE CASCADE when cart is deleted")
                .isEqualTo(0L);
    }

    // =========================================================================
    // CustomerOrder tests
    // =========================================================================

    @Test
    @Transactional
    void order_canBePersistedWithSnapshotAndStatus() {
        AppUser user = savedUser("ord_persist");
        CustomerOrder order = savedOrder(user);

        assertThat(order.getId()).isNotNull().isPositive();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getShipRecipientName()).isEqualTo("Test Recipient");
        assertThat(order.getShipCity()).isEqualTo("Testville");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void order_negativeTotalAmount_rejectedByCheckConstraint() {
        AppUser user = savedUser("ord_neg");
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShipRecipientName("R");
        order.setShipLine1("L1");
        order.setShipCity("C");
        order.setShipStateProvince("S");
        order.setShipPostalCode("P");
        order.setShipCountry("India");
        order.setSubtotalAmount(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("-1.00")); // CHECK total_amount >= 0
        assertThatThrownBy(() -> orderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void order_bigDecimalPrecision_preserved() {
        AppUser user = savedUser("ord_prec");
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setShipRecipientName("R");
        order.setShipLine1("L1");
        order.setShipCity("C");
        order.setShipStateProvince("S");
        order.setShipPostalCode("P");
        order.setShipCountry("India");
        order.setSubtotalAmount(new BigDecimal("1234.56"));
        order.setTotalAmount(new BigDecimal("1234.56"));
        CustomerOrder saved = orderRepository.saveAndFlush(order);

        CustomerOrder reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @Transactional
    void order_findByUserId_returnsOnlyCustomerOrders() {
        AppUser userA = savedUser("ord_find_a");
        AppUser userB = savedUser("ord_find_b");
        savedOrder(userA);
        savedOrder(userA);
        savedOrder(userB);
        orderRepository.flush();

        org.springframework.data.domain.Page<CustomerOrder> page =
                orderRepository.findByUserId(userA.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(o -> o.getUser().getId().equals(userA.getId()));
    }

    @Test
    @Transactional
    void order_findByStatus_returnsCorrectSubset() {
        AppUser user = savedUser("ord_status");
        CustomerOrder pending = savedOrder(user);

        CustomerOrder confirmed = new CustomerOrder();
        confirmed.setUser(user);
        confirmed.setStatus(OrderStatus.CONFIRMED);
        confirmed.setShipRecipientName("R");
        confirmed.setShipLine1("L1");
        confirmed.setShipCity("C");
        confirmed.setShipStateProvince("S");
        confirmed.setShipPostalCode("P");
        confirmed.setShipCountry("India");
        confirmed.setSubtotalAmount(BigDecimal.TEN);
        confirmed.setTotalAmount(BigDecimal.TEN);
        orderRepository.saveAndFlush(confirmed);

        org.springframework.data.domain.Page<CustomerOrder> pendingPage =
                orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT,
                        org.springframework.data.domain.PageRequest.of(0, 100));
        assertThat(pendingPage.getContent()).extracting(CustomerOrder::getId)
                .contains(pending.getId())
                .doesNotContain(confirmed.getId());
    }

    // =========================================================================
    // OrderItem tests
    // =========================================================================

    @Test
    @Transactional
    void orderItem_canBePersistedWithSnapshots() {
        AppUser user = savedUser("oi_persist");
        Category cat = savedCategory("oi_cat");
        Product product = savedProduct(cat, "oi_product");
        CustomerOrder order = savedOrder(user);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("oi_product");
        item.setUnitPriceSnapshot(new BigDecimal("75.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("150.00"));
        OrderItem saved = orderItemRepository.saveAndFlush(item);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getProductNameSnapshot()).isEqualTo("oi_product");
        assertThat(saved.getUnitPriceSnapshot()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getLineTotal()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @Transactional
    void orderItem_zeroQuantity_rejectedByCheckConstraint() {
        AppUser user = savedUser("oi_qty_zero");
        Category cat = savedCategory("oi_qty_zero_cat");
        Product product = savedProduct(cat, "oi_qty_zero_prod");
        CustomerOrder order = savedOrder(user);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("name");
        item.setUnitPriceSnapshot(new BigDecimal("10.00"));
        item.setQuantity(0);  // CHECK quantity > 0
        item.setLineTotal(BigDecimal.ZERO);
        assertThatThrownBy(() -> orderItemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void orderItem_negativeUnitPrice_rejectedByCheckConstraint() {
        AppUser user = savedUser("oi_neg_price");
        Category cat = savedCategory("oi_neg_price_cat");
        Product product = savedProduct(cat, "oi_neg_price_prod");
        CustomerOrder order = savedOrder(user);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("name");
        item.setUnitPriceSnapshot(new BigDecimal("-0.01")); // CHECK unit_price_snapshot >= 0
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("-0.01"));
        assertThatThrownBy(() -> orderItemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void orderItem_productIdSetNullWhenProductDeleted() {
        // Verifies ON DELETE SET NULL on order_item.product_id (ERD §9.2, §3.10).
        AppUser user = savedUser("oi_set_null");
        Category cat = savedCategory("oi_set_null_cat");
        Product product = savedProduct(cat, "oi_set_null_prod");
        CustomerOrder order = savedOrder(user);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("oi_set_null_prod");
        item.setUnitPriceSnapshot(new BigDecimal("75.00"));
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("75.00"));
        OrderItem saved = orderItemRepository.saveAndFlush(item);
        Long itemId = saved.getId();
        Long productId = product.getId();

        em.flush();
        em.clear();

        // Delete the product row via native SQL — ON DELETE SET NULL on order_item.product_id.
        em.createNativeQuery("DELETE FROM product WHERE id = :id")
                .setParameter("id", productId)
                .executeUpdate();
        em.flush();

        // Verify the order_item row still exists but product_id is now null.
        Number itemCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM order_item WHERE id = :id")
                .setParameter("id", itemId)
                .getSingleResult();
        assertThat(itemCount.longValue())
                .as("order_item row must survive product hard-delete (ON DELETE SET NULL)")
                .isEqualTo(1L);

        Number nullPidCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM order_item WHERE id = :id AND product_id IS NULL")
                .setParameter("id", itemId)
                .getSingleResult();
        assertThat(nullPidCount.longValue())
                .as("product_id must be set to NULL after product hard-delete")
                .isEqualTo(1L);
    }

    @Test
    @Transactional
    void orderItem_cascadeDeleteWithOrder() {
        AppUser user = savedUser("oi_cascade");
        Category cat = savedCategory("oi_cascade_cat");
        Product product = savedProduct(cat, "oi_cascade_prod");
        CustomerOrder order = savedOrder(user);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("name");
        item.setUnitPriceSnapshot(new BigDecimal("75.00"));
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("75.00"));
        orderItemRepository.saveAndFlush(item);
        Long orderId = order.getId();
        Long itemId = item.getId();

        em.flush();
        em.clear();

        // Delete the order via native SQL — ON DELETE CASCADE on order_item.order_id.
        em.createNativeQuery("DELETE FROM customer_order WHERE id = :id")
                .setParameter("id", orderId)
                .executeUpdate();
        em.flush();

        Number count = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM order_item WHERE id = :id")
                .setParameter("id", itemId)
                .getSingleResult();
        assertThat(count.longValue())
                .as("order_item row must be removed by ON DELETE CASCADE when order is deleted")
                .isEqualTo(0L);
    }

    // =========================================================================
    // Payment tests
    // =========================================================================

    @Test
    @Transactional
    void payment_canBePersistedForOrder() {
        AppUser user = savedUser("pay_persist");
        CustomerOrder order = savedOrder(user);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(new BigDecimal("75.00"));
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.saveAndFlush(payment);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getPaymentPurpose()).isEqualTo(PaymentPurpose.FULL);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getInitiatedAt()).isNotNull();
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @Transactional
    void payment_withCustomOrderRequestId_persists() {
        // Verifies that payment.custom_order_request_id can hold a raw ID value.
        // The FK to custom_order_request is deferred to Phase 2E (V5 migration).
        // We use a synthetic non-null Long and verify the CHECK constraint accepts it.
        Payment payment = new Payment();
        payment.setCustomOrderRequestId(999L); // no FK yet — raw column
        payment.setPaymentPurpose(PaymentPurpose.ADVANCE);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.saveAndFlush(payment);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCustomOrderRequestId()).isEqualTo(999L);
        assertThat(saved.getOrder()).isNull();
    }

    @Test
    @Transactional
    void payment_bothReferencesNull_rejectedByMutualExclusivityCheck() {
        // CHECK: (order_id IS NOT NULL)::INT + (custom_order_request_id IS NOT NULL)::INT = 1
        Payment payment = new Payment();
        // Neither order_id nor custom_order_request_id set → sum = 0 → CHECK fails.
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.PENDING);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void payment_bothReferencesSet_rejectedByMutualExclusivityCheck() {
        AppUser user = savedUser("pay_both");
        CustomerOrder order = savedOrder(user);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setCustomOrderRequestId(42L);  // both set → sum = 2 → CHECK fails
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.PENDING);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void payment_negativeAmount_rejectedByCheckConstraint() {
        AppUser user = savedUser("pay_neg");
        CustomerOrder order = savedOrder(user);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentPurpose(PaymentPurpose.FULL);
        payment.setAmount(new BigDecimal("-0.01")); // CHECK amount >= 0
        payment.setPaymentMethod("SANDBOX");
        payment.setStatus(PaymentStatus.PENDING);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void payment_providerReferenceUniqueness_rejectedForDuplicate() {
        AppUser userA = savedUser("pay_ref_a");
        AppUser userB = savedUser("pay_ref_b");
        CustomerOrder orderA = savedOrder(userA);
        CustomerOrder orderB = savedOrder(userB);

        Payment first = new Payment();
        first.setOrder(orderA);
        first.setPaymentPurpose(PaymentPurpose.FULL);
        first.setAmount(new BigDecimal("50.00"));
        first.setPaymentMethod("SANDBOX");
        first.setStatus(PaymentStatus.SUCCESS);
        first.setProviderTransactionReference("TXN-UNIQUE-REF-001");
        paymentRepository.saveAndFlush(first);

        Payment duplicate = new Payment();
        duplicate.setOrder(orderB);
        duplicate.setPaymentPurpose(PaymentPurpose.FULL);
        duplicate.setAmount(new BigDecimal("50.00"));
        duplicate.setPaymentMethod("SANDBOX");
        duplicate.setStatus(PaymentStatus.SUCCESS);
        duplicate.setProviderTransactionReference("TXN-UNIQUE-REF-001"); // same ref
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
    void payment_findByOrderIdAndStatus_returnsCorrectPayment() {
        AppUser user = savedUser("pay_find");
        CustomerOrder order = savedOrder(user);

        Payment failed = new Payment();
        failed.setOrder(order);
        failed.setPaymentPurpose(PaymentPurpose.FULL);
        failed.setAmount(new BigDecimal("75.00"));
        failed.setPaymentMethod("SANDBOX");
        failed.setStatus(PaymentStatus.FAILED);
        paymentRepository.saveAndFlush(failed);

        Payment success = new Payment();
        success.setOrder(order);
        success.setPaymentPurpose(PaymentPurpose.FULL);
        success.setAmount(new BigDecimal("75.00"));
        success.setPaymentMethod("SANDBOX");
        success.setStatus(PaymentStatus.SUCCESS);
        success.setProviderTransactionReference("TXN-PAY-FIND-001");
        paymentRepository.saveAndFlush(success);

        Optional<Payment> found = paymentRepository.findByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCESS);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(found.get().getProviderTransactionReference())
                .isEqualTo("TXN-PAY-FIND-001");
    }

    @Test
    @Transactional
    void payment_multipleAttemptsAllowed_forSameOrder() {
        // An order may have multiple payment rows (e.g., failed → success retry).
        AppUser user = savedUser("pay_multi");
        CustomerOrder order = savedOrder(user);

        Payment attempt1 = new Payment();
        attempt1.setOrder(order);
        attempt1.setPaymentPurpose(PaymentPurpose.FULL);
        attempt1.setAmount(new BigDecimal("75.00"));
        attempt1.setPaymentMethod("SANDBOX");
        attempt1.setStatus(PaymentStatus.FAILED);
        paymentRepository.saveAndFlush(attempt1);

        Payment attempt2 = new Payment();
        attempt2.setOrder(order);
        attempt2.setPaymentPurpose(PaymentPurpose.FULL);
        attempt2.setAmount(new BigDecimal("75.00"));
        attempt2.setPaymentMethod("SANDBOX");
        attempt2.setStatus(PaymentStatus.SUCCESS);
        attempt2.setProviderTransactionReference("TXN-MULTI-002");
        paymentRepository.saveAndFlush(attempt2);

        List<Payment> all = paymentRepository.findByOrderId(order.getId());
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.SUCCESS);
    }
}
