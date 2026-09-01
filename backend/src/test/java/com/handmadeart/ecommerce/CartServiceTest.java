package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.cart.AddCartItemRequest;
import com.handmadeart.ecommerce.dto.cart.CartResponse;
import com.handmadeart.ecommerce.dto.cart.UpdateCartItemRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Cart;
import com.handmadeart.ecommerce.entity.CartItem;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.InsufficientStockException;
import com.handmadeart.ecommerce.exception.ProductNotPurchasableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CartItemRepository;
import com.handmadeart.ecommerce.repository.CartRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.service.CartService;
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
 * Unit tests for the real {@link CartService} production logic.
 *
 * Uses Mockito only — no Spring context, no database.
 * Tests real business rules: ownership, eligibility, stock check, totals, accumulation.
 *
 * Covered:
 *   CART-S-01  getCart — no existing cart → empty CartResponse (no cart record created)
 *   CART-S-02  getCart — existing cart with items → CartResponse with items and correct total
 *   CART-S-03  addItem — new eligible READY_MADE in-stock product → item created, cart returned
 *   CART-S-04  addItem — product already in cart → quantity accumulated, no duplicate row
 *   CART-S-05  addItem — accumulated quantity exceeds stock → InsufficientStockException
 *   CART-S-06  addItem — product not found → ResourceNotFoundException
 *   CART-S-07  addItem — INACTIVE product → ProductNotPurchasableException
 *   CART-S-08  addItem — PORTFOLIO_ONLY product → ProductNotPurchasableException (not READY_MADE)
 *   CART-S-09  addItem — quantity exceeds stock → InsufficientStockException
 *   CART-S-10  addItem — no inventory row → InsufficientStockException
 *   CART-S-11  addItem — cart does not exist → lazily created
 *   CART-S-12  updateItemQuantity — valid quantity + in-stock → item updated, cart returned
 *   CART-S-13  updateItemQuantity — quantity exceeds stock → InsufficientStockException
 *   CART-S-14  updateItemQuantity — item not found → ResourceNotFoundException
 *   CART-S-15  updateItemQuantity — item belongs to different cart → ResourceNotFoundException (ownership)
 *   CART-S-16  removeItem — owned item removed, updated cart returned
 *   CART-S-17  removeItem — item not found → ResourceNotFoundException
 *   CART-S-18  removeItem — item belongs to different cart → ResourceNotFoundException
 *   CART-S-19  clearCart — items deleted, cart record preserved
 *   CART-S-20  clearCart — no cart → no-op, no exception
 *   CART-S-21  total calculation — server-calculated from current product prices (two READY_MADE items)
 *   CART-S-22  addItem — CUSTOM_AVAILABLE product → ProductNotPurchasableException (FR-CART-01)
 *   CART-S-23  price recalculation — total reflects current product price (two different READY_MADE items)
 *   CART-S-24  empty cart response — cartId is null (no cart record created) and total is zero
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartRepository, cartItemRepository, productRepository, inventoryRepository);
    }

    // =========================================================================
    // Test Fixtures
    // =========================================================================

    private AppUser buildCustomer(Long id) {
        AppUser user = new AppUser();
        user.setEmail("customer" + id + "@example.com");
        user.setFullName("Customer " + id);
        user.setRole(UserRole.CUSTOMER);
        // AppUser.id is not settable via setter — we'll use reflection for tests
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private Product buildProduct(Long id, ProductType type, ProductStatus status, BigDecimal price) {
        Product p = new Product();
        p.setProductType(type);
        p.setStatus(status);
        p.setPrice(price);
        p.setName("Product " + id);
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    private Cart buildCart(Long id, AppUser user) {
        Cart cart = new Cart();
        cart.setUser(user);
        try {
            var field = Cart.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(cart, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cart;
    }

    private CartItem buildCartItem(Long id, Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        try {
            var field = CartItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return item;
    }

    private Inventory buildInventory(Product product, int qty) {
        Inventory inv = new Inventory();
        inv.setProduct(product);
        inv.setQuantityOnHand(qty);
        return inv;
    }

    // =========================================================================
    // CART-S-01: getCart — no existing cart → empty CartResponse
    // =========================================================================

    @Test
    @DisplayName("CART-S-01: getCart when no cart exists returns empty CartResponse without creating a record")
    void getCart_noExistingCart_returnsEmptyCartResponse() {
        AppUser user = buildCustomer(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponse response = cartService.getCart(user);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        // Cart was NOT persisted
        verify(cartRepository, never()).save(any());
    }

    // =========================================================================
    // CART-S-02: getCart — existing cart with items → correct total
    // =========================================================================

    @Test
    @DisplayName("CART-S-02: getCart with items returns CartResponse with server-calculated total")
    void getCart_existingCartWithItems_returnsCartResponseWithTotal() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("25.00"));
        CartItem item = buildCartItem(100L, cart, p, 2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item));

        CartResponse response = cartService.getCart(user);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("50.00")); // 25 × 2
    }

    // =========================================================================
    // CART-S-03: addItem — new eligible READY_MADE in-stock product
    // =========================================================================

    @Test
    @DisplayName("CART-S-03: addItem with new eligible READY_MADE product creates cart item")
    void addItem_newEligibleReadyMadeProduct_createsCartItem() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("30.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 10)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(2);

        CartResponse response = cartService.addItem(user, req);

        assertThat(response).isNotNull();
        ArgumentCaptor<CartItem> savedItem = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(savedItem.capture());
        assertThat(savedItem.getValue().getQuantity()).isEqualTo(2);
    }

    // =========================================================================
    // CART-S-04: addItem — product already in cart → quantity accumulated
    // =========================================================================

    @Test
    @DisplayName("CART-S-04: addItem with existing product in cart accumulates quantity, no duplicate row")
    void addItem_productAlreadyInCart_accumulatesQuantity() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));
        CartItem existingItem = buildCartItem(100L, cart, p, 3);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(existingItem));
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 10)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(existingItem));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(2); // 3 + 2 = 5, stock = 10 → ok

        cartService.addItem(user, req);

        // Quantity should be accumulated: 3 + 2 = 5
        assertThat(existingItem.getQuantity()).isEqualTo(5);
    }

    // =========================================================================
    // CART-S-05: addItem — accumulated quantity exceeds stock → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CART-S-05: addItem when accumulated quantity exceeds stock throws InsufficientStockException")
    void addItem_accumulatedQtyExceedsStock_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));
        CartItem existingItem = buildCartItem(100L, cart, p, 3);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(existingItem));
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 4))); // only 4 on hand

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(3); // 3 + 3 = 6 > stock 4 → should throw

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    // =========================================================================
    // CART-S-06: addItem — product not found → ResourceNotFoundException
    // =========================================================================

    @Test
    @DisplayName("CART-S-06: addItem with non-existent product throws ResourceNotFoundException")
    void addItem_productNotFound_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(99L);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // CART-S-07: addItem — INACTIVE product → ProductNotPurchasableException
    // =========================================================================

    @Test
    @DisplayName("CART-S-07: addItem with INACTIVE product throws ProductNotPurchasableException")
    void addItem_inactiveProduct_throwsProductNotPurchasableException() {
        AppUser user = buildCustomer(1L);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.INACTIVE,
                new BigDecimal("10.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(ProductNotPurchasableException.class);
    }

    // =========================================================================
    // CART-S-08: addItem — PORTFOLIO_ONLY product → ProductNotPurchasableException
    // =========================================================================

    @Test
    @DisplayName("CART-S-08: addItem with PORTFOLIO_ONLY product throws ProductNotPurchasableException (not READY_MADE)")
    void addItem_portfolioOnlyProduct_throwsProductNotPurchasableException() {
        AppUser user = buildCustomer(1L);
        Product p = buildProduct(1L, ProductType.PORTFOLIO_ONLY, ProductStatus.ACTIVE,
                new BigDecimal("10.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(ProductNotPurchasableException.class)
                .hasMessageContaining("ready-made");
    }

    // =========================================================================
    // CART-S-09: addItem — quantity exceeds available stock → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CART-S-09: addItem with quantity exceeding stock throws InsufficientStockException")
    void addItem_quantityExceedsStock_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("15.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 2))); // only 2

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(5); // 5 > 2

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("5")
                .hasMessageContaining("2");
    }

    // =========================================================================
    // CART-S-10: addItem — no inventory row → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CART-S-10: addItem when no inventory row exists throws InsufficientStockException")
    void addItem_noInventoryRow_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("15.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    // =========================================================================
    // CART-S-11: addItem — cart does not exist → lazily created
    // =========================================================================

    @Test
    @DisplayName("CART-S-11: addItem creates cart lazily if none exists for the user")
    void addItem_noExistingCart_lazilyCreatesCart() {
        AppUser user = buildCustomer(1L);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("10.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        Cart newCart = buildCart(10L, user);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartItemRepository.findByCartIdAndProductId(10L, 1L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 5)));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        cartService.addItem(user, req);

        verify(cartRepository).save(any(Cart.class));
    }

    // =========================================================================
    // CART-S-12: updateItemQuantity — valid quantity + in-stock → item updated
    // =========================================================================

    @Test
    @DisplayName("CART-S-12: updateItemQuantity with valid quantity updates item and returns cart")
    void updateItemQuantity_validQuantity_updatesItem() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));
        CartItem item = buildCartItem(100L, cart, p, 1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndId(10L, 100L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 10)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(4);

        CartResponse response = cartService.updateItemQuantity(user, 100L, req);

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(response).isNotNull();
    }

    // =========================================================================
    // CART-S-13: updateItemQuantity — quantity exceeds stock → InsufficientStockException
    // =========================================================================

    @Test
    @DisplayName("CART-S-13: updateItemQuantity exceeding stock throws InsufficientStockException")
    void updateItemQuantity_exceedsStock_throwsInsufficientStockException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));
        CartItem item = buildCartItem(100L, cart, p, 1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndId(10L, 100L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(buildInventory(p, 3))); // only 3

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(10); // 10 > 3

        assertThatThrownBy(() -> cartService.updateItemQuantity(user, 100L, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    // =========================================================================
    // CART-S-14: updateItemQuantity — item not found → ResourceNotFoundException
    // =========================================================================

    @Test
    @DisplayName("CART-S-14: updateItemQuantity with non-existent item throws ResourceNotFoundException")
    void updateItemQuantity_itemNotFound_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndId(10L, 999L)).thenReturn(Optional.empty());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(2);

        assertThatThrownBy(() -> cartService.updateItemQuantity(user, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // CART-S-15: updateItemQuantity — item belongs to different cart → 404 (ownership)
    // =========================================================================

    @Test
    @DisplayName("CART-S-15: updateItemQuantity for item owned by another user returns 404 (ownership privacy)")
    void updateItemQuantity_itemBelongsToDifferentCart_throwsResourceNotFoundException() {
        AppUser user1 = buildCustomer(1L);
        Cart cart1 = buildCart(10L, user1);

        // findByCartIdAndId(cart1.id=10, itemId=200) returns empty — item 200 belongs to another cart
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart1));
        when(cartItemRepository.findByCartIdAndId(10L, 200L)).thenReturn(Optional.empty());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(2);

        assertThatThrownBy(() -> cartService.updateItemQuantity(user1, 200L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    // =========================================================================
    // CART-S-16: removeItem — owned item removed, updated cart returned
    // =========================================================================

    @Test
    @DisplayName("CART-S-16: removeItem removes owned item and returns updated cart")
    void removeItem_ownedItem_removesItemAndReturnsUpdatedCart() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);
        Product p = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("10.00"));
        CartItem item = buildCartItem(100L, cart, p, 1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndId(10L, 100L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(user, 100L);

        verify(cartItemRepository).delete(item);
        assertThat(response.getItems()).isEmpty();
    }

    // =========================================================================
    // CART-S-17: removeItem — item not found → ResourceNotFoundException
    // =========================================================================

    @Test
    @DisplayName("CART-S-17: removeItem with non-existent item throws ResourceNotFoundException")
    void removeItem_itemNotFound_throwsResourceNotFoundException() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndId(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(user, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // CART-S-18: removeItem — item belongs to different cart → 404 (ownership)
    // =========================================================================

    @Test
    @DisplayName("CART-S-18: removeItem for item owned by another user returns 404 (ownership privacy)")
    void removeItem_itemBelongsToDifferentCart_throwsResourceNotFoundException() {
        AppUser user1 = buildCustomer(1L);
        Cart cart1 = buildCart(10L, user1);

        // findByCartIdAndId(cart1.id=10, itemId=200) returns empty — item 200 belongs to another cart
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart1));
        when(cartItemRepository.findByCartIdAndId(10L, 200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(user1, 200L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    // =========================================================================
    // CART-S-19: clearCart — items deleted, cart record preserved
    // =========================================================================

    @Test
    @DisplayName("CART-S-19: clearCart deletes all cart items but preserves the cart record")
    void clearCart_existingCart_deletesItemsPreservesCartRecord() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.clearCart(user);

        verify(cartItemRepository).deleteByCartId(10L);
        verify(cartRepository, never()).delete(any());
    }

    // =========================================================================
    // CART-S-20: clearCart — no cart → no-op, no exception
    // =========================================================================

    @Test
    @DisplayName("CART-S-20: clearCart when no cart exists is a no-op and does not throw")
    void clearCart_noExistingCart_noOp() {
        AppUser user = buildCustomer(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Should not throw
        cartService.clearCart(user);

        verify(cartItemRepository, never()).deleteByCartId(anyLong());
    }

    // =========================================================================
    // CART-S-21: total calculation — server-calculated from current product prices
    // =========================================================================

    @Test
    @DisplayName("CART-S-21: cart total is server-calculated from current product prices (two READY_MADE items)")
    void cartTotal_isServerCalculatedFromCurrentProductPrice() {
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);

        Product p1 = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("10.00"));
        Product p2 = buildProduct(2L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("25.50"));
        CartItem item1 = buildCartItem(100L, cart, p1, 2);  // 10.00 × 2 = 20.00
        CartItem item2 = buildCartItem(101L, cart, p2, 1);  // 25.50 × 1 = 25.50

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item1, item2));

        CartResponse response = cartService.getCart(user);

        // Total = 20.00 + 25.50 = 45.50
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("45.50"));
        assertThat(response.getItems()).hasSize(2);
    }

    // =========================================================================
    // CART-S-22: addItem — CUSTOM_AVAILABLE product → ProductNotPurchasableException
    // =========================================================================

    @Test
    @DisplayName("CART-S-22: addItem with CUSTOM_AVAILABLE product throws ProductNotPurchasableException (FR-CART-01)")
    void addItem_customAvailableProduct_throwsProductNotPurchasableException() {
        // FR-CART-01: only READY_MADE products may be added to cart.
        // CUSTOM_AVAILABLE follows the custom-artwork commissioned workflow, not the cart flow.
        AppUser user = buildCustomer(1L);
        Product p = buildProduct(1L, ProductType.CUSTOM_AVAILABLE, ProductStatus.ACTIVE,
                new BigDecimal("150.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(user, req))
                .isInstanceOf(ProductNotPurchasableException.class)
                .hasMessageContaining("ready-made");
    }

    // =========================================================================
    // CART-S-23: price recalculation — total reflects current product price
    // =========================================================================

    @Test
    @DisplayName("CART-S-23: total is recalculated from current product price each time cart is loaded")
    void getCart_totalReflectsCurrentProductPrice_notStaleCachedPrice() {
        // Two READY_MADE items; total is derived from current product.price, not any
        // persisted value (there is no persisted price in cart_item — DB Design §8.3).
        AppUser user = buildCustomer(1L);
        Cart cart = buildCart(10L, user);

        // Product prices set at "current" values
        Product p1 = buildProduct(1L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("50.00"));  // current price
        Product p2 = buildProduct(2L, ProductType.READY_MADE, ProductStatus.ACTIVE,
                new BigDecimal("20.00"));  // current price
        CartItem item1 = buildCartItem(100L, cart, p1, 3);  // 50.00 × 3 = 150.00
        CartItem item2 = buildCartItem(101L, cart, p2, 2);  // 20.00 × 2 = 40.00

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(item1, item2));

        CartResponse response = cartService.getCart(user);

        // Total = 150.00 + 40.00 = 190.00 — computed from item.getProduct().getPrice()
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("190.00"));
        // Item subtotals also reflect current prices
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).anySatisfy(item ->
                assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("150.00")));
        assertThat(response.getItems()).anySatisfy(item ->
                assertThat(item.getSubtotal()).isEqualByComparingTo(new BigDecimal("40.00")));
    }

    // =========================================================================
    // CART-S-24: empty cart response — cartId null, total zero
    // =========================================================================

    @Test
    @DisplayName("CART-S-24: getCart when no cart exists returns CartResponse with null cartId and zero total")
    void getCart_noExistingCart_cartIdIsNullAndTotalIsZero() {
        // Per approved cart lifecycle: cart is created lazily on first addItem.
        // A customer who has never added a product has no cart record.
        // getCart must return an empty view without persisting anything.
        AppUser user = buildCustomer(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponse response = cartService.getCart(user);

        assertThat(response.getCartId()).isNull();
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getItems()).isEmpty();
        // No cart record created
        verify(cartRepository, never()).save(any());
    }
}
