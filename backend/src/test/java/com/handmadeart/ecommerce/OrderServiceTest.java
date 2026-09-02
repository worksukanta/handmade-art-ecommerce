package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.dto.order.OrderSummaryResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import com.handmadeart.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderService} business logic.
 *
 * Covered:
 *   ORD-S-01  getOrderHistory returns paginated customer orders
 *   ORD-S-02  getOrderHistory returns empty page if no orders
 *   ORD-S-03  getOrderDetail returns full OrderResponse with item snapshots
 *   ORD-S-04  getOrderDetail with foreign orderId → ResourceNotFoundException (non-disclosure)
 *   ORD-S-05  getOrderDetail item snapshots — productName and unitPrice from snapshots, not live catalogue
 *   ORD-S-06  getOrderHistory uses authenticated user id (ownership enforced by repository)
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CustomerOrderRepository customerOrderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShipmentRepository shipmentRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(customerOrderRepository, orderItemRepository, shipmentRepository);
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

    private CustomerOrder buildOrder(Long id, AppUser user, OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(status);
        order.setShipRecipientName("Alice Smith");
        order.setShipLine1("10 Main Street");
        order.setShipCity("London");
        order.setShipStateProvince("England");
        order.setShipPostalCode("SW1A 1AA");
        order.setShipCountry("United Kingdom");
        order.setSubtotalAmount(new BigDecimal("50.00"));
        order.setTotalAmount(new BigDecimal("50.00"));
        setId(order, CustomerOrder.class, id);
        return order;
    }

    private OrderItem buildOrderItem(Long id, CustomerOrder order) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductNameSnapshot("Snapshot Product");
        item.setUnitPriceSnapshot(new BigDecimal("50.00"));
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("50.00"));
        setId(item, OrderItem.class, id);
        return item;
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
    // ORD-S-01: getOrderHistory returns paginated customer orders
    // =========================================================================

    @Test
    @DisplayName("ORD-S-01: getOrderHistory returns paginated orders for the authenticated customer")
    void getOrderHistory_returnsPaginatedOrders() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order1 = buildOrder(10L, user, OrderStatus.CONFIRMED);
        CustomerOrder order2 = buildOrder(11L, user, OrderStatus.PENDING_PAYMENT);

        when(customerOrderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order1, order2)));

        PageResponse<OrderSummaryResponse> result = orderService.getOrderHistory(user, 0, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(10L);
        assertThat(result.getContent().get(1).getOrderId()).isEqualTo(11L);
    }

    // =========================================================================
    // ORD-S-02: getOrderHistory with no orders returns empty page
    // =========================================================================

    @Test
    @DisplayName("ORD-S-02: getOrderHistory returns empty page when customer has no orders")
    void getOrderHistory_empty_returnsEmptyPage() {
        AppUser user = buildCustomer(1L);

        when(customerOrderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<OrderSummaryResponse> result = orderService.getOrderHistory(user, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // =========================================================================
    // ORD-S-03: getOrderDetail returns full OrderResponse
    // =========================================================================

    @Test
    @DisplayName("ORD-S-03: getOrderDetail returns full OrderResponse with items")
    void getOrderDetail_returnsOrderWithItems() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.CONFIRMED);
        OrderItem item = buildOrderItem(100L, order);

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        OrderResponse result = orderService.getOrderDetail(user, 10L);

        assertThat(result.getOrderId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getItems()).hasSize(1);
    }

    // =========================================================================
    // ORD-S-04: getOrderDetail with foreign orderId → 404 (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("ORD-S-04: getOrderDetail with foreign orderId returns 404 (non-disclosure)")
    void getOrderDetail_foreignOrderId_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);

        // findByUserIdAndId returns empty for foreign/missing orderId
        when(customerOrderRepository.findByUserIdAndId(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderDetail(user, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // =========================================================================
    // ORD-S-05: getOrderDetail returns snapshot values, not live product data
    // =========================================================================

    @Test
    @DisplayName("ORD-S-05: getOrderDetail returns item snapshots (productName/unitPrice) not live catalogue")
    void getOrderDetail_itemSnapshotValues_notLiveCatalogue() {
        AppUser user = buildCustomer(1L);
        CustomerOrder order = buildOrder(10L, user, OrderStatus.CONFIRMED);

        // Build item with explicit snapshot values
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductNameSnapshot("Snapshot Name at Purchase Time");
        item.setUnitPriceSnapshot(new BigDecimal("75.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("150.00"));
        // product FK intentionally null — simulates product deleted after purchase
        item.setProduct(null);
        setId(item, OrderItem.class, 100L);

        when(customerOrderRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(item));

        OrderResponse result = orderService.getOrderDetail(user, 10L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Snapshot Name at Purchase Time");
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.getItems().get(0).getLineTotal()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getItems().get(0).getProductId()).isNull();
    }

    // =========================================================================
    // ORD-S-06: getOrderHistory scoped to authenticated user's id
    // =========================================================================

    @Test
    @DisplayName("ORD-S-06: getOrderHistory only queries orders for the authenticated user's id")
    void getOrderHistory_usesAuthenticatedUserIdNotClientSupplied() {
        AppUser user = buildCustomer(42L);

        when(customerOrderRepository.findByUserId(eq(42L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getOrderHistory(user, 0, 20);

        // Verify the repository was called with the correct user id (not some other id)
        org.mockito.Mockito.verify(customerOrderRepository).findByUserId(eq(42L), any(Pageable.class));
    }
}
