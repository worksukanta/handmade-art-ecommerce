-- =============================================================================
-- V4__create_commerce_tables.sql
--
-- Migration: Commerce tables — Cart, CartItem, CustomerOrder, OrderItem, Payment.
-- Phase 2D — Commerce Database Model.
--
-- Creates:
--   cart            — one active cart per customer
--   cart_item       — line items in a cart (product + quantity)
--   customer_order  — confirmed purchase records
--   order_item      — per-product purchase-time snapshot within an order
--   payment         — payment transaction records (ready-made + custom artwork)
--
-- Database Design & ERD: §3.7 (Cart), §3.8 (CartItem), §3.9 (Order),
--                        §3.10 (OrderItem), §3.11 (Payment), §8–10, §15–17
--
-- Notes:
--   - payment.custom_order_request_id column is created here without a FK
--     constraint because the custom_order_request table does not yet exist
--     (Phase 2E). The FK will be added in V5 when that table is created.
--   - customer_order uses table name 'customer_order' (not 'order') because
--     ORDER is a reserved SQL keyword.
--   - All timestamp columns use DB DEFAULT now() as the authoritative source;
--     application code uses @Generated to read them back after INSERT/UPDATE.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Table: cart
--
-- One active cart per customer.
-- user_id is UNIQUE NOT NULL, enforcing the one-cart-per-customer rule (ERD §8.1).
-- No status column — no saved/multiple-cart workflow is approved for MVP (ERD §15.6).
-- -----------------------------------------------------------------------------
CREATE TABLE cart (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL
                           REFERENCES app_user (id)
                           ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- UNIQUE index: one cart per customer + natural lookup path for 'get my cart'.
CREATE UNIQUE INDEX uq_cart_user_id
    ON cart (user_id);


-- -----------------------------------------------------------------------------
-- Table: cart_item
--
-- Line items within a cart. A product appears at most once per cart (UNIQUE
-- cart_id + product_id); quantity is updated in place rather than creating
-- duplicate rows (ERD §8.2, FR-CART-02).
-- CartItem does NOT store price — a cart must always reflect the live product
-- price (ERD §8.3, §6.1).
-- ON DELETE CASCADE: removing a cart removes all its items (ERD §16).
-- -----------------------------------------------------------------------------
CREATE TABLE cart_item (
    id         BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id    BIGINT  NOT NULL
                       REFERENCES cart (id)
                       ON DELETE CASCADE,
    product_id BIGINT  NOT NULL
                       REFERENCES product (id)
                       ON DELETE RESTRICT,
    quantity   INTEGER NOT NULL
                       CHECK (quantity > 0),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index: load all items for a given cart efficiently (ERD §17).
CREATE INDEX idx_cart_item_cart_id
    ON cart_item (cart_id);

-- Unique constraint: prevents duplicate product rows in the same cart (ERD §3.8).
CREATE UNIQUE INDEX uq_cart_item_cart_product
    ON cart_item (cart_id, product_id);


-- -----------------------------------------------------------------------------
-- Table: customer_order
--
-- Confirmed ready-made purchase records.
-- Named 'customer_order' because ORDER is a reserved SQL keyword.
-- ship_* columns are a checkout-time snapshot of the selected delivery address —
-- not a FK to address, so later address edits/deletes don't affect history (ERD §9.3).
-- subtotal_amount and total_amount are server-computed (FR-CART-04/07, BR-10).
-- Tax and delivery columns are intentionally absent (DEC-007 DEFERRED, ERD §9.4).
-- -----------------------------------------------------------------------------
CREATE TABLE customer_order (
    id                   BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT         NOT NULL
                                        REFERENCES app_user (id)
                                        ON DELETE RESTRICT,
    status               VARCHAR(20)    NOT NULL DEFAULT 'PENDING_PAYMENT'
                                        CHECK (status IN (
                                            'PENDING_PAYMENT',
                                            'CONFIRMED',
                                            'PROCESSING',
                                            'SHIPPED',
                                            'DELIVERED',
                                            'CANCELLED'
                                        )),
    -- Shipping address snapshot (ERD §9.3, §6.1) ----------------------------
    ship_recipient_name  VARCHAR(150)   NOT NULL,
    ship_line1           VARCHAR(255)   NOT NULL,
    ship_line2           VARCHAR(255),
    ship_city            VARCHAR(100)   NOT NULL,
    ship_state_province  VARCHAR(100)   NOT NULL,
    ship_postal_code     VARCHAR(20)    NOT NULL,
    ship_country         VARCHAR(100)   NOT NULL,
    ship_phone           VARCHAR(20),
    -- Totals (server-authoritative, FR-CART-04/07, BR-10) -------------------
    subtotal_amount      NUMERIC(10, 2) NOT NULL
                                        CHECK (subtotal_amount >= 0),
    total_amount         NUMERIC(10, 2) NOT NULL
                                        CHECK (total_amount >= 0),
    -- Audit timestamps -------------------------------------------------------
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Index: 'My Orders' history query by customer (UC-009, ERD §17).
CREATE INDEX idx_customer_order_user_id
    ON customer_order (user_id);

-- Index: Admin order-management filtering by status (UC-010, UC-018, ERD §17).
CREATE INDEX idx_customer_order_status
    ON customer_order (status);


-- -----------------------------------------------------------------------------
-- Table: order_item
--
-- One row per product per order.
-- purchase-time name and price snapshots (BR-11, FR-ORD-03) ensure historical
-- order records remain accurate if a product is later renamed, repriced, or deleted.
-- product_id: nullable FK with ON DELETE SET NULL — historical rows survive a product
-- hard-delete (ERD §9.2, §3.10).
-- ON DELETE CASCADE on order_id: removing an order removes all its items (ERD §16).
-- -----------------------------------------------------------------------------
CREATE TABLE order_item (
    id                      BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                BIGINT         NOT NULL
                                           REFERENCES customer_order (id)
                                           ON DELETE CASCADE,
    product_id              BIGINT
                                           REFERENCES product (id)
                                           ON DELETE SET NULL,
    product_name_snapshot   VARCHAR(200)   NOT NULL,
    unit_price_snapshot     NUMERIC(10, 2) NOT NULL
                                           CHECK (unit_price_snapshot >= 0),
    quantity                INTEGER        NOT NULL
                                           CHECK (quantity > 0),
    line_total              NUMERIC(10, 2) NOT NULL
                                           CHECK (line_total >= 0)
);

-- Index: load all items for a given order efficiently (ERD §17).
CREATE INDEX idx_order_item_order_id
    ON order_item (order_id);


-- -----------------------------------------------------------------------------
-- Table: payment
--
-- Payment transaction records for both ready-made orders and custom-artwork
-- advance/remaining payments. A single table serves both journeys via two
-- nullable FKs (order_id, custom_order_request_id); exactly one must be set
-- per row (ERD §10.2, §3.11).
--
-- Security: never stores card number, CVV, PIN or other card secrets (FR-PAY-04,
-- BR-13, NFR-07, DEC-001 DEFERRED) — only provider_transaction_reference,
-- amount, status, and method label are stored.
--
-- Note on custom_order_request_id FK:
--   The custom_order_request table is created in Phase 2E (V5 migration).
--   This column is created here as a plain BIGINT without a FK constraint.
--   The FK to custom_order_request(id) will be added in V5 once that table
--   exists. The CHECK mutual-exclusivity constraint still applies.
-- -----------------------------------------------------------------------------
CREATE TABLE payment (
    id                              BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                        BIGINT
                                               REFERENCES customer_order (id)
                                               ON DELETE RESTRICT,
    -- FK to custom_order_request added in V5 when Phase 2E table is created.
    custom_order_request_id         BIGINT,
    -- Mutual-exclusivity constraint: exactly one of the two references must be set.
    CONSTRAINT chk_payment_single_owner
        CHECK (
            (order_id IS NOT NULL)::INT + (custom_order_request_id IS NOT NULL)::INT = 1
        ),
    payment_purpose                 VARCHAR(15)    NOT NULL
                                               CHECK (payment_purpose IN ('FULL', 'ADVANCE', 'REMAINING')),
    amount                          NUMERIC(10, 2) NOT NULL
                                               CHECK (amount >= 0),
    payment_method                  VARCHAR(30)    NOT NULL,
    status                          VARCHAR(10)    NOT NULL DEFAULT 'PENDING'
                                               CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    provider_transaction_reference  VARCHAR(150),
    failure_reason                  VARCHAR(255),
    initiated_at                    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    completed_at                    TIMESTAMPTZ
);

-- Index: payment lookup by order (ERD §17).
CREATE INDEX idx_payment_order_id
    ON payment (order_id);

-- Index: payment lookup by custom order request (ERD §17).
CREATE INDEX idx_payment_custom_order_request_id
    ON payment (custom_order_request_id);

-- Partial unique index: prevents two payment rows claiming the same provider
-- reference, while allowing NULL for not-yet-confirmed payments (ERD §16).
CREATE UNIQUE INDEX uq_payment_provider_reference
    ON payment (provider_transaction_reference)
    WHERE provider_transaction_reference IS NOT NULL;


-- =============================================================================
-- End of V4__create_commerce_tables.sql
-- =============================================================================
