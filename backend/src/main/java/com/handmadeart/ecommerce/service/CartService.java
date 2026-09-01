package com.handmadeart.ecommerce.service;

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
import com.handmadeart.ecommerce.exception.InsufficientStockException;
import com.handmadeart.ecommerce.exception.ProductNotPurchasableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CartItemRepository;
import com.handmadeart.ecommerce.repository.CartRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for customer cart operations.
 *
 * Ownership enforcement:
 *   The authenticated customer is resolved by the calling controller via
 *   {@link CurrentUserService}. Every operation below takes the resolved
 *   {@link AppUser} and never accepts a client-supplied user ID or cart ID.
 *
 * Cart lifecycle (per approved database design):
 *   - One cart per customer (UNIQUE user_id DB constraint).
 *   - Lazy creation: a cart is created on the first add-to-cart operation if
 *     the customer does not yet have one (approved DB design note in Cart entity).
 *   - Clearing cart items does NOT delete the cart record.
 *
 * Product eligibility (SRS FR-CART-01, REST API Spec §18, REST API Spec §8 Add-cart-item purpose):
 *   "An authenticated customer shall be able to add an available ready-made product to a cart."
 *   "Only eligible ready-made products can be added to cart/purchased."
 *   A product is cart-eligible when:
 *     - product_type = READY_MADE   (FR-CART-01; REST API Spec §18/§8)
 *     - status = ACTIVE
 *
 *   CUSTOM_AVAILABLE products are NOT cart-eligible: they follow the custom-artwork
 *   commissioned workflow (SDD §10.A, SRS §6.6), not the ready-made cart/checkout flow.
 *   PORTFOLIO_ONLY products are display-only and never purchasable.
 *
 * Inventory check at cart time (REST API Spec §8 "409 unavailable/stock rule",
 *   REST API Spec §12 note; TC-029):
 *   - Quantity requested must not exceed quantity_on_hand.
 *   - No stock is reserved; the check is advisory at cart stage.
 *   - DEC-009 (inventory concurrency) remains OPEN; no locking is applied here.
 *
 * Duplicate product in cart:
 *   - UNIQUE(cart_id, product_id) DB constraint prevents duplicate rows.
 *   - If a product already in the cart is added again, its quantity is increased
 *     by the requested amount (accumulation semantics).
 *
 * Pricing:
 *   - CartItem has no persisted price (approved DB design §8.3).
 *   - Totals are computed from the current authoritative product price at
 *     response time and never accepted from client input.
 *
 * DEC-007 (tax/delivery): DEFERRED — totals contain item subtotals only.
 * DEC-009 (inventory concurrency): OPEN — no locking.
 */
@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       InventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // =========================================================================
    // GET /api/v1/cart
    // =========================================================================

    /**
     * Return the current cart for the authenticated customer.
     *
     * Cart lifecycle: if the customer has never added a product, no cart row exists.
     * This operation returns an empty cart representation without creating a persistent
     * cart record — a record is only created on the first add-to-cart (lazy creation).
     *
     * @param currentUser authenticated customer (never a client-supplied user ID)
     * @return CartResponse containing items and server-calculated totals
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(AppUser currentUser) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getId());
        if (cartOpt.isEmpty()) {
            // No cart yet — return a stable empty cart view without persisting anything
            Cart emptyPlaceholder = buildEmptyCartPlaceholder(currentUser);
            return CartResponse.from(emptyPlaceholder, List.of());
        }
        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.from(cart, items);
    }

    // =========================================================================
    // POST /api/v1/cart/items
    // =========================================================================

    /**
     * Add a product to the authenticated customer's cart.
     *
     * Rules:
     *   1. Product must exist and be ACTIVE (404 if not found, 409 if INACTIVE/PORTFOLIO_ONLY).
     *   2. Product type must not be PORTFOLIO_ONLY (409).
     *   3. Quantity must be >= 1 (validated by @Min on request DTO, 400).
     *   4. Available stock must be >= requested quantity (409 insufficient stock).
     *      - Only READY_MADE / CUSTOM_AVAILABLE have inventory rows.
     *   5. If the product is already in the cart, quantity is accumulated (not duplicated).
     *      Combined quantity must also pass the stock check.
     *   6. Cart is lazily created on first add if it does not yet exist.
     *
     * @param currentUser authenticated customer
     * @param request     addCartItemRequest (productId, quantity — validated)
     * @return CartResponse with updated items and server-calculated totals
     */
    public CartResponse addItem(AppUser currentUser, AddCartItemRequest request) {
        Product product = requirePurchasableProduct(request.getProductId());
        int requestedQty = request.getQuantity();

        // Resolve or lazily create the cart
        Cart cart = getOrCreateCart(currentUser);

        // Check if this product is already in the cart
        Optional<CartItem> existingItem =
                cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            // Accumulate quantity
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + requestedQty;
            checkStockAvailability(product, newQty);
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            // New line item
            checkStockAvailability(product, requestedQty);
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(requestedQty);
            cartItemRepository.save(item);
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.from(cart, items);
    }

    // =========================================================================
    // PUT /api/v1/cart/items/{itemId}
    // =========================================================================

    /**
     * Update the quantity of an existing cart item owned by the authenticated customer.
     *
     * Ownership: the itemId is verified to belong to the current user's cart.
     * Any mismatch returns 404 (does not leak existence of another user's item).
     *
     * @param currentUser authenticated customer
     * @param itemId      cart_item.id (path parameter)
     * @param request     updateCartItemRequest (quantity >= 1, validated)
     * @return CartResponse with updated items and server-calculated totals
     */
    public CartResponse updateItemQuantity(AppUser currentUser, Long itemId,
                                           UpdateCartItemRequest request) {
        Cart cart = requireCart(currentUser);
        CartItem item = requireOwnedItem(cart, itemId);

        checkStockAvailability(item.getProduct(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.from(cart, items);
    }

    // =========================================================================
    // DELETE /api/v1/cart/items/{itemId}
    // =========================================================================

    /**
     * Remove a single cart item owned by the authenticated customer.
     *
     * Ownership: the itemId is verified to belong to the current user's cart.
     * Any mismatch returns 404.
     *
     * @param currentUser authenticated customer
     * @param itemId      cart_item.id (path parameter)
     * @return CartResponse with the remaining items and updated totals
     */
    public CartResponse removeItem(AppUser currentUser, Long itemId) {
        Cart cart = requireCart(currentUser);
        CartItem item = requireOwnedItem(cart, itemId);

        cartItemRepository.delete(item);

        List<CartItem> remaining = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.from(cart, remaining);
    }

    // =========================================================================
    // DELETE /api/v1/cart/items
    // =========================================================================

    /**
     * Clear all items from the authenticated customer's cart.
     *
     * The cart record itself is NOT deleted — only cart_item rows are removed.
     * This matches the approved database design: clearing items ≠ deleting the cart.
     *
     * If the customer has no cart, this is a no-op (204 returned by controller).
     *
     * @param currentUser authenticated customer
     */
    public void clearCart(AppUser currentUser) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getId());
        cartOpt.ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Look up a product, verify it exists, is ACTIVE, and is READY_MADE.
     *
     * Cart eligibility rule (SRS FR-CART-01; REST API Spec §18 §8):
     *   Only READY_MADE + ACTIVE products may be added to cart.
     *   - CUSTOM_AVAILABLE follows the custom-artwork commissioned workflow, not cart.
     *   - PORTFOLIO_ONLY is display-only; never purchasable.
     *   - INACTIVE products are not available for purchase.
     *
     * @param productId the product ID from the request
     * @return the verified Product entity
     * @throws ResourceNotFoundException      if product does not exist
     * @throws ProductNotPurchasableException if product is not READY_MADE or not ACTIVE
     */
    private Product requirePurchasableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        if (product.getProductType() != ProductType.READY_MADE) {
            // CUSTOM_AVAILABLE → custom-artwork workflow; PORTFOLIO_ONLY → display only
            throw new ProductNotPurchasableException(
                    "Only ready-made products can be added to cart");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotPurchasableException(
                    "Product is not available for purchase");
        }
        return product;
    }

    /**
     * Check that the requested quantity does not exceed available stock.
     *
     * Called only after {@link #requirePurchasableProduct} has confirmed the product
     * is READY_MADE + ACTIVE. READY_MADE products always have an inventory row.
     * DEC-009 (inventory concurrency) is OPEN; no locking is applied.
     *
     * @param product      the product to check (must be READY_MADE)
     * @param requestedQty the total quantity desired (may be accumulated)
     * @throws InsufficientStockException if quantity exceeds stock on hand or no inventory row
     */
    private void checkStockAvailability(Product product, int requestedQty) {
        if (product.getProductType() != ProductType.READY_MADE) {
            // Defensive guard — should not reach here after requirePurchasableProduct
            throw new ProductNotPurchasableException(
                    "Only ready-made products can be added to cart");
        }
        Optional<Inventory> invOpt = inventoryRepository.findByProductId(product.getId());
        if (invOpt.isEmpty()) {
            // No inventory row means not in stock
            throw new InsufficientStockException(
                    "Product is not available in stock");
        }
        int onHand = invOpt.get().getQuantityOnHand();
        if (requestedQty > onHand) {
            throw new InsufficientStockException(
                    "Requested quantity " + requestedQty
                            + " exceeds available stock (" + onHand + ")");
        }
    }

    /**
     * Retrieve the cart for the given user, creating it if it does not exist.
     *
     * The UNIQUE(user_id) database constraint prevents double-creation even
     * in sequential concurrent calls on the same DB connection.
     */
    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Require the cart to exist for the given user.
     * Used for update/remove operations where the cart must already exist.
     *
     * @throws ResourceNotFoundException if the user has no cart (and therefore no items)
     */
    private Cart requireCart(AppUser user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found"));
    }

    /**
     * Require a cart item that belongs to the given cart.
     *
     * The query is scoped through the user's cartId — a foreign item ID can never
     * be resolved even before the application-level check, which satisfies ownership
     * privacy without a separate ownership-check step.
     *
     * Returns 404 for both "item does not exist" and "item belongs to a different cart"
     * (approved non-disclosure semantics: do not reveal another user's item existence).
     *
     * @param cart   the current user's cart (ownership anchor)
     * @param itemId the requested item ID
     * @return the CartItem entity
     * @throws ResourceNotFoundException if the item does not exist or does not belong to this cart
     */
    private CartItem requireOwnedItem(Cart cart, Long itemId) {
        return cartItemRepository.findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    /**
     * Build a transient (non-persisted) Cart placeholder for customers who have no cart.
     * Used solely for building an empty CartResponse without DB side effects.
     */
    private Cart buildEmptyCartPlaceholder(AppUser user) {
        Cart placeholder = new Cart();
        placeholder.setUser(user);
        // id remains null — CartResponse.cartId will be null for an empty pre-creation cart
        return placeholder;
    }
}
