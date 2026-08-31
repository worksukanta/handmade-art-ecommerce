package com.handmadeart.ecommerce.entity;

/**
 * Status values for the {@code category} table.
 *
 * Approved values from Database Design &amp; ERD §3.3:
 *   ACTIVE   — category is visible in navigation.
 *   INACTIVE — deactivated by Admin (ADM-03); hidden from customers.
 *
 * Persisted as a VARCHAR(10) CHECK-constrained column with EnumType.STRING.
 */
public enum CategoryStatus {

    ACTIVE,
    INACTIVE
}
