package com.handmadeart.ecommerce.entity;

/**
 * Application role values for AppUser.
 *
 * Exactly two roles are defined by the approved SRS/MVP Scope:
 *   CUSTOMER — standard registered user.
 *   ADMIN    — platform administrator.
 *
 * No additional roles are introduced.
 *
 * Persisted as a VARCHAR(10) CHECK-constrained column on the app_user table
 * (Database Design &amp; ERD §3.1, §6.2).  Using @Enumerated(EnumType.STRING) in
 * the entity keeps the stored value readable and removes any dependency on
 * enum ordinal order.
 */
public enum UserRole {

    CUSTOMER,
    ADMIN
}
