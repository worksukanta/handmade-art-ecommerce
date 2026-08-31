-- =============================================================================
-- V2__create_identity_tables.sql
--
-- Migration: Identity and Customer Address tables.
-- Phase 2B — Identity and Customer Database Model.
--
-- Creates:
--   app_user  — platform user accounts (CUSTOMER and ADMIN roles)
--   address   — customer delivery addresses
--
-- Database Design & ERD:  §3.1 (AppUser), §3.2 (Address), §14, §17
-- Requirements:  FR-AUTH-01..08, FR-AUTH-07, FR-CART-06
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Table: app_user
--
-- Stores both CUSTOMER and ADMIN accounts.
-- Role is a CHECK-constrained VARCHAR, not a separate table (ERD §6.2).
-- password_hash stores BCrypt output only — plaintext is never persisted.
-- email uniqueness is case-insensitive, enforced via the functional index below.
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(254)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    full_name     VARCHAR(150)    NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(10)     NOT NULL DEFAULT 'CUSTOMER'
                                  CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Case-insensitive unique index on email (ERD §3.1, §17).
-- lower(email) ensures 'User@Example.com' and 'user@example.com' are treated
-- as the same login identifier (FR-AUTH-01/02).
CREATE UNIQUE INDEX uq_app_user_email_lower
    ON app_user (lower(email));

-- Index: support fast role-based lookups (Admin identification).
CREATE INDEX idx_app_user_role
    ON app_user (role);


-- -----------------------------------------------------------------------------
-- Table: address
--
-- Customer delivery addresses.  Each row belongs to exactly one AppUser.
-- A customer may have zero or more addresses.
-- Default-address uniqueness: enforced by partial unique index below (ERD §14.2).
-- -----------------------------------------------------------------------------
CREATE TABLE address (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT       NOT NULL
                                REFERENCES app_user (id)
                                ON DELETE CASCADE,
    recipient_name VARCHAR(150) NOT NULL,
    line1          VARCHAR(255) NOT NULL,
    line2          VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    state_province VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    country        VARCHAR(100) NOT NULL,
    phone          VARCHAR(20),
    is_default     BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index: fast address lookup by customer (UC-003 — 'My Addresses', ERD §17).
CREATE INDEX idx_address_user_id
    ON address (user_id);

-- Partial unique index: enforce "at most one default address per customer" (ERD §14.2).
-- Using a partial index (WHERE is_default = true) is the idiomatic PostgreSQL way to
-- express this constraint without a separate lookup table or a trigger.
CREATE UNIQUE INDEX uq_address_user_default
    ON address (user_id)
    WHERE is_default = true;

-- =============================================================================
-- End of V2__create_identity_tables.sql
-- =============================================================================
