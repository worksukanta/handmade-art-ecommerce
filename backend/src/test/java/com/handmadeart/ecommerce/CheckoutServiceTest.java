package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.order.CreateOrderRequest;
import com.handmadeart.ecommerce.dto.order.OrderResponse;
import com.handmadeart.ecommerce.entity.Address;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Cart;
import com.handmadeart.ecommerce.entity.CartItem;
import com.handmadeart.ecommerce.entity.CustomerOrder;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.OrderItem;
import com.handmadeart.ecommerce.entity.OrderStatus;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.EmptyCartException;
import com.handmadeart.ecommerce.exception.InsufficientStockException;
import com.handmadeart.ecommerce.exception.ProductNotPurchasableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.AddressRepository;
import com.handmadeart.ecommerce.repository.CartItemRepository;
import com.handmadeart.ecommerce.repository.CartRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.service.CheckoutService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CheckoutService} business logic.
 *
 * Uses Mockito only — no Spring context, no database.
 * Verifies the complete checkout flow: address ownership, empty-cart guard,
 * product eligibility re-validation, pessimistic inventory lock usage,
 * stock sufficiency, order/snapshot creation, inventory decrement, cart clearing.
 *
 * Covered:
 *   CHK-S-01  successful checkout → order created, inventory decremented, cart cleared
 *   CHK-S-02  empty cart → EmptyCartException
 *   CHK-S-03  no cart at all → EmptyCartException
 *   CHK-S-04  foreign/missing address → ResourceNotFoundException (non-disclosure)
 *   CHK-S-05  INACTIVE product at checkout → ProductNotPurchasableException
 *   CHK-S-06  PORTFOLIO_ONLY product at checkout → ProductNotPurchasableException
 *   CHK-S-07  insufficient stock at checkout → InsufficientStockException
 *   CHK-S-08  no inventory row at checkout → InsufficientStockException
 *   CHK-S-09  findByProductIdWithLock is used (not non-locking findByProductId)
 *   CHK-S-10  order item snapshots use purchase-time name and price
 *   CHK-S-11  total is server-calculated from current product price (BigDecimal)
 *   CHK-S-12  failure before order save does not proceed to inventory decrement
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CustomerOrderRepository customerOrderRepository;
    @Mock private OrderItemRepository orderItemRepository;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                cartRepository, cartItemRepository, addressRepository,
                inventoryRepository, customerOrderRepository, orderItemRepository);
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

    private Address buildAddress(Long id, AppUser owner) {
        Address addr = new Address();
        addr.setUser(owner);
        addr.setRecipientName("Alice Smith");
        addr.setLine1("10 Main Street");
        addr.setLine2(null);
        addr.setCity("London");
        addr.setStateProvince("England");
        addr.setPostalCode("SW1A 1AA");
        addr.setCountry("United Kingdom");
        addr.setPhone("+44 7700 900000");
        setId(addr, Address.class, id);
        return addr;
    }

    private Product buildProduct(Long id, ProductType type, ProductStatus status, BigDecimal price) {
        Product p = new Product();
        p.setProductType(type);
        p.setStatus(status);
        p.setPrice(price);
        p.setName("Product " + id);
        setId(p, Product.class, id);
        return p;
    }

    private Cart buildCart(Long id, AppUser user) {
        Cart cart = new Cart();
        cart.setUser(user);
        setId(cart, Cart.class, id);
        return cart;
    }

    private CartItem buildCartItem(Long id, Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        setId(item, CartItem.class, id);
        return item;
    }

    private Inventory buildInventory(Product product, int qty) {
        Inventory inv = new Inventory();
        inv.setProduct(product);
        inv.setQuantityOnHand(qty);
        return inv;
    }

    private CustomerOrder buildSavedOrder(Long id, AppUser user) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShipRecipientName("Alice Smith");
        order.setShipLine1("10 Main Street");
        order.setShipCity("London");
        order.setShipStateProvince("England");
        order.setShipPostalCode("SW1A 1AA");
        order.setShipCountry("United Kingdom");
        order.setSubtotalAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        setId(order, CustomerOrder.class, id);
        return order;
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
    // CHK-S-01: Successful checkout
    // =========================================================================

    @Test
    @DisplayName("CHK-S-01: successful checkout creates order, decrements inventory, clears cart")
    void createOrder_success_createsOrderDecrementsInventoryClearsCart() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("50.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 2);
        Inventory inv = buildInventory(p, 10);
        CustomerOrder savedOrder = buildSavedOrder(99L, user);
        savedOrder.setSubtotalAmount(new BigDecimal("100.00"));
        savedOrder.setTotalAmount(new BigDecimal("100.00"));

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inv));
        when(customerOrderRepository.save(any())).thenReturn(savedOrder);
        OrderItem savedItem = new OrderItem();
        savedItem.setOrder(savedOrder);
        savedItem.setProduct(p);
        savedItem.setProductNameSnapshot("Product 1");
        savedItem.setUnitPriceSnapshot(new BigDecimal("50.00"));
        savedItem.setQuantity(2);
        savedItem.setLineTotal(new BigDecimal("100.00"));
        when(orderItemRepository.save(any())).thenReturn(savedItem);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        OrderResponse response = checkoutService.createOrder(user, req);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(99L);
        // Inventory decremented: 10 - 2 = 8
        assertThat(inv.getQuantityOnHand()).isEqualTo(8);
        // Cart cleared
        verify(cartItemRepository).deleteByCartId(20L);
        // Order item saved
        verify(orderItemRepository).save(any());
    }

    // =========================================================================
    // CHK-S-02: Empty cart → EmptyCartException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-02: checkout with empty cart throws EmptyCartException")
    void createOrder_emptyCart_throwsEmptyCartException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(EmptyCartException.class);
    }

    // =========================================================================
    // CHK-S-03: No cart at all → EmptyCartException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-03: checkout with no cart record throws EmptyCartException")
    void createOrder_noCart_throwsEmptyCartException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(EmptyCartException.class);
    }

    // =========================================================================
    // CHK-S-04: Foreign/missing address → ResourceNotFoundException (non-disclosure)
    // =========================================================================

    @Test
    @DisplayName("CHK-S-04: foreign/missing addressId returns 404 (non-disclosure)")
    void createOrder_foreignAddress_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);
        // findByUserIdAndId returns empty — address doesn't exist or belongs to another user
        when(addressRepository.findByUserIdAndId(1L, 999L)).thenReturn(Optional.empty());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(999L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");
    }

    // =========================================================================
    // CHK-S-05: INACTIVE product at checkout → ProductNotPurchasableException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-05: INACTIVE product at checkout throws ProductNotPurchasableException")
    void createOrder_inactiveProduct_throwsProductNotPurchasableException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.INACTIVE,
                new BigDecimal("20.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 1);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(ProductNotPurchasableException.class);
    }

    // =========================================================================
    // CHK-S-06: PORTFOLIO_ONLY product → ProductNotPurchasableException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-06: PORTFOLIO_ONLY product at checkout throws ProductNotPurchasableException")
    void createOrder_portfolioOnlyProduct_throwsProductNotPurchasableException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.PORTFOLIO_ONLY, ProductStatus.ACTIVE,
                new BigDecimal("0.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 1);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(ProductNotPurchasableException.class);
    }

    // =========================================================================
    // CHK-S-07: Insufficient stock at checkout → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-07: insufficient stock at checkout throws InsufficientStockException")
    void createOrder_insufficientStock_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("30.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 5);
        Inventory inv = buildInventory(p, 3); // only 3 available

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inv));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    // =========================================================================
    // CHK-S-08: No inventory row at checkout → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CHK-S-08: no inventory row at checkout throws InsufficientStockException")
    void createOrder_noInventoryRow_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("30.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 1);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.empty());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    // =========================================================================
    // CHK-S-09: Pessimistic lock method used (not the non-locking findByProductId)
    // =========================================================================

    @Test
    @DisplayName("CHK-S-09: findByProductIdWithLock is called (pessimistic lock path, DEC-009)")
    void createOrder_usesPessimisticLockRepository() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("10.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 1);
        Inventory inv = buildInventory(p, 5);
        CustomerOrder savedOrder = buildSavedOrder(99L, user);
        savedOrder.setSubtotalAmount(new BigDecimal("10.00"));
        savedOrder.setTotalAmount(new BigDecimal("10.00"));

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inv));
        when(customerOrderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);
        checkoutService.createOrder(user, req);

        // Verify the locking method was called, not the non-locking method
        verify(inventoryRepository).findByProductIdWithLock(1L);
        verify(inventoryRepository, never()).findByProductId(anyLong());
    }

    // =========================================================================
    // CHK-S-10: Order item snapshots use purchase-time name and price
    // =========================================================================

    @Test
    @DisplayName("CHK-S-10: order item snapshots capture purchase-time name and price")
    void createOrder_orderItemSnapshotsCapturePurchaseTimeValues() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("75.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 3);
        Inventory inv = buildInventory(p, 10);
        CustomerOrder savedOrder = buildSavedOrder(99L, user);
        savedOrder.setSubtotalAmount(new BigDecimal("225.00"));
        savedOrder.setTotalAmount(new BigDecimal("225.00"));

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inv));
        when(customerOrderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);
        checkoutService.createOrder(user, req);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem captured = itemCaptor.getValue();

        assertThat(captured.getProductNameSnapshot()).isEqualTo("Product 1");
        assertThat(captured.getUnitPriceSnapshot()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(captured.getQuantity()).isEqualTo(3);
        assertThat(captured.getLineTotal()).isEqualByComparingTo(new BigDecimal("225.00"));
    }

    // =========================================================================
    // CHK-S-11: Total is server-calculated from current product price (BigDecimal)
    // =========================================================================

    @Test
    @DisplayName("CHK-S-11: order total is server-calculated from current product.price, not client input")
    void createOrder_totalServerCalculatedFromCurrentProductPrice() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        Product p1 = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));
        Product p2 = buildProduct(2L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("15.00"));
        CartItem ci1 = buildCartItem(100L, cart, p1, 2);  // 40.00
        CartItem ci2 = buildCartItem(101L, cart, p2, 3);  // 45.00
        Inventory inv1 = buildInventory(p1, 10);
        Inventory inv2 = buildInventory(p2, 10);
        CustomerOrder savedOrder = buildSavedOrder(99L, user);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(ci1, ci2));
        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.findByProductIdWithLock(2L)).thenReturn(Optional.of(inv2));
        when(customerOrderRepository.save(any())).thenAnswer(invoc -> {
            CustomerOrder o = invoc.getArgument(0);
            setId(o, CustomerOrder.class, 99L);
            return o;
        });
        when(orderItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);
        checkoutService.createOrder(user, req);

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(customerOrderRepository).save(orderCaptor.capture());
        CustomerOrder captured = orderCaptor.getValue();

        // Total = 20.00×2 + 15.00×3 = 40.00 + 45.00 = 85.00
        assertThat(captured.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("85.00"));
        assertThat(captured.getTotalAmount()).isEqualByComparingTo(new BigDecimal("85.00"));
    }

    // =========================================================================
    // CHK-S-12: Failure before order save does not proceed to inventory decrement
    // =========================================================================

    @Test
    @DisplayName("CHK-S-12: failure during eligibility check does not proceed to inventory decrement")
    void createOrder_failureBeforeOrderSave_doesNotDecrementInventory() {
        AppUser user = buildCustomer(1L);
        Address addr = buildAddress(10L, user);
        Cart cart = buildCart(20L, user);
        // INACTIVE product — fails eligibility before inventory lock
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.INACTIVE,
                new BigDecimal("10.00"));
        CartItem cartItem = buildCartItem(100L, cart, p, 1);

        when(addressRepository.findByUserIdAndId(1L, 10L)).thenReturn(Optional.of(addr));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(20L)).thenReturn(List.of(cartItem));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(10L);

        assertThatThrownBy(() -> checkoutService.createOrder(user, req))
                .isInstanceOf(ProductNotPurchasableException.class);

        // Inventory never touched
        verify(inventoryRepository, never()).findByProductIdWithLock(anyLong());
        verify(customerOrderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByCartId(anyLong());
    }
}
