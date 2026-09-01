package com.handmadeart.ecommerce.exception;

/**
 * Thrown when a customer attempts to add a product to cart that is not eligible
 * for purchase — either INACTIVE status or PORTFOLIO_ONLY type.
 *
 * Mapped to 409 Conflict by {@link GlobalExceptionHandler}
 * (REST API Spec §8 "Add cart item" Errors: 400 invalid quantity/type, 409 unavailable/stock rule).
 *
 * Cart eligibility rule (REST API Spec §18):
 *   "Only eligible ready-made products can be added to cart/purchased."
 *   READY_MADE + ACTIVE: purchasable.
 *   CUSTOM_AVAILABLE + ACTIVE: purchasable.
 *   PORTFOLIO_ONLY: never purchasable.
 *   INACTIVE: not purchasable.
 */
public class ProductNotPurchasableException extends RuntimeException {

    public ProductNotPurchasableException(String message) {
        super(message);
    }
}
