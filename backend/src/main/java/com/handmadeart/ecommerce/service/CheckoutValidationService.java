package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.order.CheckoutValidationResponse;
import com.handmadeart.ecommerce.dto.order.CheckoutValidationResponse.ValidationItemSummary;
import com.handmadeart.ecommerce.dto.order.CreateOrderRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CartItem;
import com.handmadeart.ecommerce.entity.Inventory;
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
import com.handmadeart.ecommerce.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for POST /api/v1/checkout/validate — pre-order validation.
 *
 * NON-MUTATING CONTRACT:
 *   This service MUST NOT create any order, order items, payment records,
 *   decrement inventory, clear the cart, or acquire pessimistic write locks.
 *   It performs advisory read-only validation using the same rules as actual
 *   checkout, but without the side effects.
 *
 *   Actual checkout (CheckoutService.createOrder) remains authoritative and
 *   re-validates under DEC-009 pessimistic locking before committing.
 *
 * Validation rules (mirrors CheckoutService eligibility):
 *   - Address must be owned by the authenticated customer.
 *   - Cart must exist and be non-empty.
 *   - Every cart item's product must be READY_MADE + ACTIVE.
 *   - Current inventory must be sufficient for the requested quantities.
 *   - Totals are computed server-side from current product prices (DEC-007 DEFERRED).
 *
 * No pessimistic lock is acquired (read-only advisory validation).
 */
@Service
public class CheckoutValidationService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;

    public CheckoutValidationService(CartRepository cartRepository,
                                     CartItemRepository cartItemRepository,
                                     AddressRepository addressRepository,
                                     InventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Validate the cart and address for checkout without mutating any state.
     *
     * @param currentUser authenticated customer (JWT-derived; never client-supplied)
     * @param request     contains the explicitly chosen owned addressId
     * @return CheckoutValidationResponse with item summaries and server-computed totals
     * @throws ResourceNotFoundException     if address is not owned by the customer
     * @throws EmptyCartException            if the cart is empty or does not exist
     * @throws ProductNotPurchasableException if any product is not checkout-eligible
     * @throws InsufficientStockException    if stock is insufficient for any item
     */
    @Transactional(readOnly = true)
    public CheckoutValidationResponse validate(AppUser currentUser, CreateOrderRequest request) {

        // Verify address ownership (same non-disclosure semantics as checkout)
        addressRepository
                .findByUserIdAndId(currentUser.getId(), request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Resolve cart items
        List<CartItem> cartItems = cartRepository
                .findByUserId(currentUser.getId())
                .map(cart -> cartItemRepository.findByCartId(cart.getId()))
                .orElse(List.of());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        // Validate each item — same eligibility rules as actual checkout
        List<ValidationItemSummary> summaries = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Re-validate product eligibility (mirrors CheckoutService)
            if (product.getProductType() != ProductType.READY_MADE) {
                throw new ProductNotPurchasableException(
                        "Product '" + product.getName() + "' is not eligible for checkout");
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ProductNotPurchasableException(
                        "Product '" + product.getName() + "' is no longer available");
            }

            // Advisory stock check (no pessimistic lock — read-only)
            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() -> new InsufficientStockException(
                            "Product '" + product.getName() + "' is not in stock"));

            if (cartItem.getQuantity() > inventory.getQuantityOnHand()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + product.getName()
                                + "': requested " + cartItem.getQuantity()
                                + ", available " + inventory.getQuantityOnHand());
            }

            // Build item summary with server-authoritative price
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            ValidationItemSummary summary = new ValidationItemSummary();
            summary.setProductId(product.getId());
            summary.setProductName(product.getName());
            summary.setQuantity(cartItem.getQuantity());
            summary.setUnitPrice(unitPrice);
            summary.setLineTotal(lineTotal);
            summaries.add(summary);
        }

        // Build response — DEC-007 DEFERRED: totalAmount = subtotalAmount
        CheckoutValidationResponse response = new CheckoutValidationResponse();
        response.setValid(true);
        response.setItems(summaries);
        response.setSubtotalAmount(subtotal);
        response.setTotalAmount(subtotal);
        return response;
    }
}
