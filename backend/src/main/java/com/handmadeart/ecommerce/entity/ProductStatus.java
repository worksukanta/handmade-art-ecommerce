package com.handmadeart.ecommerce.entity;

/**
 * Status values for the {@code product} table.
 *
 * Approved values from Database Design &amp; ERD §3.4:
 *   ACTIVE   — visible and (for purchasable types) orderable in the customer catalogue.
 *   INACTIVE — deactivated by Admin (FR-PROD-03/08); hidden from customers but
 *              retained for historical order references.
 *
 * Persisted as a VARCHAR(10) CHECK-constrained column with EnumType.STRING.
 */
public enum ProductStatus {

    ACTIVE,
    INACTIVE
}
