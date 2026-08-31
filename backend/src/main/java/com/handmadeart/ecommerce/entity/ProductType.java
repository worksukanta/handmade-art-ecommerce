package com.handmadeart.ecommerce.entity;

/**
 * Product type values for the {@code product} table.
 *
 * Approved values from Database Design &amp; ERD §3.4 (FR-PROD-06):
 *   READY_MADE        — purchasable, ready-made artwork in stock.
 *   CUSTOM_AVAILABLE  — can be commissioned as a custom artwork request.
 *   PORTFOLIO_ONLY    — display-only; not purchasable and does not require inventory.
 *
 * Persisted as a VARCHAR(20) CHECK-constrained column with EnumType.STRING.
 */
public enum ProductType {

    READY_MADE,
    CUSTOM_AVAILABLE,
    PORTFOLIO_ONLY
}
