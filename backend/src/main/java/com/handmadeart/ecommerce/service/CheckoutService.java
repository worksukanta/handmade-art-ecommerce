package com.handmadeart.ecommerce.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that implements the standard ready-made checkout / order-creation flow.
 *
 * Transaction boundary (DEC-009 APPROVED — checkout-time pessimistic locking):
 *   The {@link #createOrder} method is a single {@code @Transactional} unit covering:
 *     1. Address ownership verification
 *     2. Cart non-empty check
 *     3. Product eligibility re-validation for every cart item
 *     4. Pessimistic inventory lock acquisition (SELECT … FOR UPDATE via JPA)
 *     5. Stock sufficiency re-validation while locks are held
 *     6. CustomerOrder creation (status = PENDING_PAYMENT)
 *     7. OrderItem creation with immutable purchase-time snapshots
 *     8. Inventory decrement
 *     9. Cart items cleared
 *    10. Transaction commit
 *
 *   If any step fails the transaction rolls back completely — no partial order,
 *   no inventory decrement, no cart change is persisted.
 *
 * Ownership enforcement:
 *   The authenticated CUSTOMER is supplied by the controller via
 *   {@link CurrentUserService}. No client-supplied user ID or cart ID is trusted.
 *
 * Address handling (DEC-010 DEFERRED):
 *   The client must explicitly supply an owned addressId.
 *   No silent fallback to a default address.
 *   Foreign/missing address → 404 (same non-disclosure semantics as other owned resources).
 *
 * Product eligibility:
 *   Re-validated at checkout time using the same READY_MADE + ACTIVE rule as cart.
 *   CUSTOM_AVAILABLE, PORTFOLIO_ONLY, INACTIVE products abort the checkout.
 *
 * Pricing:
 *   Server-authoritative. Totals computed from current product.price at checkout time.
 *   Snapshot price captured in OrderItem.unitPriceSnapshot.
 *   DEC-007 DEFERRED: totalAmount = subtotalAmount (no tax/delivery).
 */
@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public CheckoutService(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           AddressRepository addressRepository,
                           InventoryRepository inventoryRepository,
                           CustomerOrderRepository customerOrderRepository,
                           OrderItemRepository orderItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // =========================================================================
    // POST /api/v1/orders — create order from cart
    // =========================================================================

    /**
     * Create a ready-made order from the authenticated customer's cart.
     *
     * All steps execute in one transaction. Any failure rolls back entirely.
     *
     * @param currentUser authenticated customer (never a client-supplied identity)
     * @param request     contains the explicitly chosen owned addressId
     * @return OrderResponse with the persisted order data and item snapshots
     */
    @Transactional
    public OrderResponse createOrder(AppUser currentUser, CreateOrderRequest request) {

        // Step 1: Resolve and verify the customer owns the supplied address
        Address address = addressRepository
                .findByUserIdAndId(currentUser.getId(), request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found"));

        // Step 2: Resolve the cart — empty / no-cart both mean empty cart
        List<CartItem> cartItems = resolveCartItems(currentUser);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        // Step 3 + 4 + 5: For each cart item, re-validate eligibility and
        // acquire a pessimistic write lock on the inventory row, then revalidate stock.
        List<LockedItemContext> lockedItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Re-validate product eligibility at checkout time
            requireCheckoutEligibleProduct(product);

            // Acquire pessimistic write lock on inventory row (DEC-009)
            Inventory inventory = inventoryRepository
                    .findByProductIdWithLock(product.getId())
                    .orElseThrow(() -> new InsufficientStockException(
                            "Product '" + product.getName() + "' is not in stock"));

            // Re-validate quantity while lock is held
            if (cartItem.getQuantity() > inventory.getQuantityOnHand()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + product.getName()
                                + "': requested " + cartItem.getQuantity()
                                + ", available " + inventory.getQuantityOnHand());
            }

            lockedItems.add(new LockedItemContext(cartItem, inventory));
        }

        // Step 6: Create the CustomerOrder — address fields are snapshot-copied
        CustomerOrder order = buildOrder(currentUser, address);

        // Compute subtotal from current authoritative product prices (BigDecimal)
        BigDecimal subtotal = lockedItems.stream()
                .map(ctx -> ctx.cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(ctx.cartItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal);   // DEC-007 DEFERRED: no tax/delivery
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        // Step 7: Create immutable OrderItem snapshots
        List<OrderItem> savedItems = new ArrayList<>();
        for (LockedItemContext ctx : lockedItems) {
            Product product = ctx.cartItem.getProduct();
            BigDecimal unitPrice = product.getPrice();
            int qty = ctx.cartItem.getQuantity();

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setUnitPriceSnapshot(unitPrice);
            item.setQuantity(qty);
            item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
            savedItems.add(orderItemRepository.save(item));
        }

        // Step 8: Decrement inventory atomically (locks still held)
        for (LockedItemContext ctx : lockedItems) {
            ctx.inventory.setQuantityOnHand(
                    ctx.inventory.getQuantityOnHand() - ctx.cartItem.getQuantity());
            inventoryRepository.save(ctx.inventory);
        }

        // Step 9: Clear cart items (cart record preserved per Phase 3C behavior)
        Cart cart = cartRepository.findByUserId(currentUser.getId()).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }

        // Step 10: Return response — transaction commits on method exit
        return OrderResponse.from(savedOrder, savedItems);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolve the authenticated customer's cart items.
     * Returns an empty list if the customer has no cart or an empty cart.
     */
    private List<CartItem> resolveCartItems(AppUser user) {
        return cartRepository.findByUserId(user.getId())
                .map(cart -> cartItemRepository.findByCartId(cart.getId()))
                .orElse(List.of());
    }

    /**
     * Verify the product is eligible for the standard ready-made checkout.
     *
     * Checkout eligibility mirrors cart eligibility (FR-CART-01):
     *   - productType == READY_MADE
     *   - status == ACTIVE
     *
     * CUSTOM_AVAILABLE follows the custom-artwork workflow; PORTFOLIO_ONLY is display-only.
     * Both are rejected here in case product state changed since the item was added to cart.
     */
    private void requireCheckoutEligibleProduct(Product product) {
        if (product.getProductType() != ProductType.READY_MADE) {
            throw new ProductNotPurchasableException(
                    "Product '" + product.getName() + "' is not eligible for checkout");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotPurchasableException(
                    "Product '" + product.getName() + "' is no longer available");
        }
    }

    /**
     * Build a new {@link CustomerOrder} with address snapshot fields populated
     * and status PENDING_PAYMENT. Not yet persisted.
     */
    private CustomerOrder buildOrder(AppUser user, Address address) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        // Snapshot address fields so later edits/deletions of Address do not
        // affect historical order data (Database Design §9.3)
        order.setShipRecipientName(address.getRecipientName());
        order.setShipLine1(address.getLine1());
        order.setShipLine2(address.getLine2());
        order.setShipCity(address.getCity());
        order.setShipStateProvince(address.getStateProvince());
        order.setShipPostalCode(address.getPostalCode());
        order.setShipCountry(address.getCountry());
        order.setShipPhone(address.getPhone());

        return order;
    }

    // =========================================================================
    // Internal value holder for locked inventory + cart item pair
    // =========================================================================

    private static final class LockedItemContext {
        final CartItem cartItem;
        final Inventory inventory;

        LockedItemContext(CartItem cartItem, Inventory inventory) {
            this.cartItem = cartItem;
            this.inventory = inventory;
        }
    }
}
