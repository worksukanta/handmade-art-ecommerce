-- =============================================================================
-- V5__create_custom_artwork_tables.sql
--
-- Migration: Custom Artwork tables — CustomOrderRequest, CustomOrderImage,
--             Quotation, Shipment.
-- Phase 2E — Custom Artwork Database Model.
--
-- Creates:
--   custom_order_request  — commissioned artwork request and its lifecycle
--   custom_order_image    — reference image metadata for a request
--   quotation             — commercial proposal for a custom request (one per request)
--   shipment              — fulfilment/shipping record (ready-made order OR custom request)
--
-- Also adds the deferred FK that was intentionally omitted from V4:
--   payment.custom_order_request_id → custom_order_request(id)
--
-- Database Design & ERD: §3.13 (CustomOrderRequest), §3.14 (CustomOrderImage),
--                        §3.15 (Quotation), §3.16 (Shipment),
--                        §12–13, §15.3–15.4, §15.7, §16–17
--
-- Notes:
--   - custom_order_request.status carries 13 approved lifecycle values (ERD §15.3).
--   - quotation.custom_order_request_id is UNIQUE — one quotation per request (ERD §13.2,
--     DEC-004 DEFERRED).
--   - quotation.advance_amount is nullable with no fixed percentage (DEC-005 OPEN,
--     ERD §13.4).
--   - shipment uses the same dual-nullable-FK + mutual-exclusivity CHECK pattern as
--     payment (ERD §10.2, §3.16).
--   - payment.custom_order_request_id FK was deferred from V4 to avoid creating the FK
--     before the referenced table existed; it is added here via ALTER TABLE.
--   - All timestamp columns use DB DEFAULT now() as the authoritative source;
--     application code uses @Generated to read them back after INSERT/UPDATE.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Table: custom_order_request
--
-- Commissioned artwork request and full lifecycle status (SRS §8.2, FR-CUST-01..11).
-- status carries 13 approved values defined in ERD §15.3.
-- reviewed_by: nullable FK → admin who actioned the request (FR-CUST-05).
-- ON DELETE RESTRICT on user_id and reviewed_by — a user/admin cannot be deleted
-- while referenced custom requests exist.
-- -----------------------------------------------------------------------------
CREATE TABLE custom_order_request (
    id                      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 BIGINT       NOT NULL
                                         REFERENCES app_user (id)
                                         ON DELETE RESTRICT,
    product_type            VARCHAR(100) NOT NULL,
    description             TEXT         NOT NULL,
    design_theme            VARCHAR(200),
    preferred_colors        VARCHAR(200),
    dimensions_size         VARCHAR(100),
    budget_range            VARCHAR(100),
    required_delivery_date  DATE,
    additional_instructions TEXT,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED'
                                         CHECK (status IN (
                                             'REQUESTED',
                                             'UNDER_REVIEW',
                                             'QUOTED',
                                             'CUSTOMER_APPROVAL_PENDING',
                                             'APPROVED',
                                             'ADVANCE_PAYMENT_PENDING',
                                             'IN_PRODUCTION',
                                             'COMPLETED',
                                             'SHIPPED',
                                             'DELIVERED',
                                             'REJECTED',
                                             'QUOTATION_EXPIRED',
                                             'CANCELLED'
                                         )),
    reviewed_by             BIGINT
                                         REFERENCES app_user (id)
                                         ON DELETE RESTRICT,
    review_notes            TEXT,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index: 'My Custom Requests' queries by customer (UC-013, ERD §17).
CREATE INDEX idx_custom_order_request_user_id
    ON custom_order_request (user_id);

-- Index: Admin review queue filtering by status (UC-012, ERD §17).
CREATE INDEX idx_custom_order_request_status
    ON custom_order_request (status);


-- -----------------------------------------------------------------------------
-- Table: custom_order_image
--
-- Reference image metadata for a custom artwork request (FR-CUST-03, BR-14).
-- Stores file metadata only — binary files are stored on the filesystem (SDD §13).
-- ON DELETE CASCADE: deleting a request removes all its reference images (ERD §16).
-- -----------------------------------------------------------------------------
CREATE TABLE custom_order_image (
    id                        BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    custom_order_request_id   BIGINT      NOT NULL
                                          REFERENCES custom_order_request (id)
                                          ON DELETE CASCADE,
    storage_reference         VARCHAR(500) NOT NULL,
    original_filename         VARCHAR(255),
    content_type              VARCHAR(100) NOT NULL,
    file_size_bytes           INTEGER      NOT NULL
                                          CHECK (file_size_bytes > 0),
    uploaded_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index: load all reference images for a given request (ERD §17 FK index rule).
CREATE INDEX idx_custom_order_image_request_id
    ON custom_order_image (custom_order_request_id);


-- -----------------------------------------------------------------------------
-- Table: quotation
--
-- Commercial proposal for a custom artwork request (FR-CUST-07..10, BR-06).
-- One quotation per request — custom_order_request_id is UNIQUE (ERD §13.2,
-- DEC-004 DEFERRED: re-quotation not implemented).
-- advance_amount: nullable absolute value entered by Admin — no fixed percentage
-- (DEC-005 OPEN, ERD §13.4).
-- expiry_at: NOT NULL; service layer compares against now() on approval attempt
-- (ERD §13.3, BR-06, FR-CUST-10).
-- created_by: Admin who issued the quotation (FR-CUST-07) — RESTRICT.
-- ON DELETE RESTRICT on custom_order_request_id — a request with a quotation
-- cannot be hard-deleted outright.
-- -----------------------------------------------------------------------------
CREATE TABLE quotation (
    id                        BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    custom_order_request_id   BIGINT         NOT NULL
                                             REFERENCES custom_order_request (id)
                                             ON DELETE RESTRICT,
    quoted_amount             NUMERIC(10, 2) NOT NULL
                                             CHECK (quoted_amount >= 0),
    advance_amount            NUMERIC(10, 2)
                                             CHECK (advance_amount IS NULL OR advance_amount >= 0),
    estimated_delivery_date   DATE,
    expiry_at                 TIMESTAMPTZ    NOT NULL,
    notes_terms               TEXT,
    status                    VARCHAR(10)    NOT NULL DEFAULT 'PENDING'
                                             CHECK (status IN (
                                                 'PENDING',
                                                 'APPROVED',
                                                 'REJECTED',
                                                 'EXPIRED'
                                             )),
    created_by                BIGINT         NOT NULL
                                             REFERENCES app_user (id)
                                             ON DELETE RESTRICT,
    created_at                TIMESTAMPTZ    NOT NULL DEFAULT now(),
    decided_at                TIMESTAMPTZ
);

-- UNIQUE constraint: one quotation per request (ERD §13.2, DEC-004 DEFERRED).
CREATE UNIQUE INDEX uq_quotation_custom_order_request_id
    ON quotation (custom_order_request_id);

-- Composite index: find pending quotations whose expiry_at has passed (BR-06,
-- FR-CUST-10, ERD §17).
CREATE INDEX idx_quotation_status_expiry
    ON quotation (status, expiry_at);


-- -----------------------------------------------------------------------------
-- Table: shipment
--
-- Fulfilment/shipping record for a completed ready-made order OR a completed
-- custom-artwork commission. Exactly one of (order_id, custom_order_request_id)
-- must be set per row — enforced by the mutual-exclusivity CHECK constraint
-- (mirrors the payment table pattern, ERD §3.16, §10.2).
-- MVP: carrier_name and tracking_reference are free-text (DEC-008 APPROVED —
-- no automated carrier API required, FR-SHIP-04).
-- shipped_at / delivered_at: application-managed nullables (ERD §3.16).
-- ON DELETE RESTRICT on both FKs — an order or request with a shipment record
-- cannot be hard-deleted outright.
-- -----------------------------------------------------------------------------
CREATE TABLE shipment (
    id                        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                  BIGINT
                                           REFERENCES customer_order (id)
                                           ON DELETE RESTRICT,
    custom_order_request_id   BIGINT
                                           REFERENCES custom_order_request (id)
                                           ON DELETE RESTRICT,
    -- Mutual-exclusivity: exactly one of the two references must be set (ERD §3.16).
    CONSTRAINT chk_shipment_single_owner
        CHECK (
            (order_id IS NOT NULL)::INT + (custom_order_request_id IS NOT NULL)::INT = 1
        ),
    carrier_name              VARCHAR(100),
    tracking_reference        VARCHAR(150),
    status                    VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                                           CHECK (status IN (
                                               'PENDING',
                                               'SHIPPED',
                                               'DELIVERED'
                                           )),
    estimated_delivery_date   DATE,
    shipped_at                TIMESTAMPTZ,
    delivered_at              TIMESTAMPTZ,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index: look up shipment for a given ready-made order (ERD §17 FK index rule).
CREATE INDEX idx_shipment_order_id
    ON shipment (order_id);

-- Index: look up shipment for a given custom order request (ERD §17 FK index rule).
CREATE INDEX idx_shipment_custom_order_request_id
    ON shipment (custom_order_request_id);


-- -----------------------------------------------------------------------------
-- Deferred FK from V4: payment.custom_order_request_id → custom_order_request(id)
--
-- This FK was intentionally omitted in V4 because the custom_order_request table
-- did not yet exist. Now that the table is created above, the constraint is added.
-- ON DELETE RESTRICT: a custom request with payment rows cannot be deleted outright.
-- -----------------------------------------------------------------------------
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_custom_order_request
        FOREIGN KEY (custom_order_request_id)
        REFERENCES custom_order_request (id)
        ON DELETE RESTRICT;


-- =============================================================================
-- End of V5__create_custom_artwork_tables.sql
-- =============================================================================
