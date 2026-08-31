-- =============================================================================
-- V3__create_catalogue_inventory_tables.sql
--
-- Migration: Catalogue and Inventory tables.
-- Phase 2C — Catalogue and Inventory Database Model.
--
-- Creates:
--   category        — catalogue classification
--   product         — ready-made, custom-available, and portfolio artwork items
--   product_image   — file metadata for product images
--   product_related — junction: curated related-product pairs
--   inventory       — stock levels for purchasable products
--
-- Database Design & ERD: §3.3 (Category), §3.4 (Product), §3.5 (ProductImage),
--                        §3.6 (ProductRelated), §3.12 (Inventory), §7, §11, §17
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Table: category
--
-- Catalogue classifications.  No parent-category hierarchy is defined in the
-- approved schema.  Products reference a category via FK (ON DELETE RESTRICT).
-- -----------------------------------------------------------------------------
CREATE TABLE category (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Unique category names (ERD §3.3).
CREATE UNIQUE INDEX uq_category_name
    ON category (name);


-- -----------------------------------------------------------------------------
-- Table: product
--
-- Ready-made, custom-available, and portfolio-only artwork items.
-- Each product belongs to exactly one category.
-- -----------------------------------------------------------------------------
CREATE TABLE product (
    id           BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id  BIGINT          NOT NULL
                                 REFERENCES category (id)
                                 ON DELETE RESTRICT,
    name         VARCHAR(200)    NOT NULL,
    description  TEXT,
    price        NUMERIC(10, 2)  NOT NULL
                                 CHECK (price >= 0),
    product_type VARCHAR(20)     NOT NULL
                                 CHECK (product_type IN ('READY_MADE', 'CUSTOM_AVAILABLE', 'PORTFOLIO_ONLY')),
    status       VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Index: category-filtered catalogue browsing (ERD §17, FR-CAT-02/03).
CREATE INDEX idx_product_category_id
    ON product (category_id);

-- Composite index: customer catalogue always filters by status='ACTIVE' and often
-- by product_type; a composite index serves both together (ERD §17).
CREATE INDEX idx_product_status_type
    ON product (status, product_type);

-- Index: product name search/sort (ERD §17, FR-PROD-08, FR-CAT-06).
CREATE INDEX idx_product_name
    ON product (name);


-- -----------------------------------------------------------------------------
-- Table: product_image
--
-- Image file metadata for products.  Binary files live on the filesystem;
-- only path/key metadata is stored here (SDD §13).
-- ON DELETE CASCADE: removing a product removes all its image records (ERD §16).
-- -----------------------------------------------------------------------------
CREATE TABLE product_image (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id         BIGINT       NOT NULL
                                    REFERENCES product (id)
                                    ON DELETE CASCADE,
    storage_reference  VARCHAR(500) NOT NULL,
    original_filename  VARCHAR(255),
    content_type       VARCHAR(100) NOT NULL,
    file_size_bytes    INTEGER      NOT NULL
                                    CHECK (file_size_bytes > 0),
    display_order      INTEGER      NOT NULL DEFAULT 0,
    is_primary         BOOLEAN      NOT NULL DEFAULT false,
    uploaded_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index: load all images for a product efficiently (ERD §17 — FKs implied index rule).
CREATE INDEX idx_product_image_product_id
    ON product_image (product_id);


-- -----------------------------------------------------------------------------
-- Table: product_related  (junction)
--
-- Curated, directional related-product pairs.
-- Composite PK (product_id, related_product_id) prevents duplicate pairs.
-- CHECK prevents a product from being related to itself (ERD §3.6).
-- ON DELETE CASCADE on both FKs: removing either product removes the relation (ERD §16).
-- -----------------------------------------------------------------------------
CREATE TABLE product_related (
    product_id         BIGINT      NOT NULL
                                   REFERENCES product (id)
                                   ON DELETE CASCADE,
    related_product_id BIGINT      NOT NULL
                                   REFERENCES product (id)
                                   ON DELETE CASCADE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_product_related PRIMARY KEY (product_id, related_product_id),
    CONSTRAINT chk_product_related_no_self
        CHECK (product_id <> related_product_id)
);

-- Index on related_product_id for reverse-direction lookups (ERD §17 — FK index rule).
CREATE INDEX idx_product_related_related_id
    ON product_related (related_product_id);


-- -----------------------------------------------------------------------------
-- Table: inventory
--
-- Stock levels for READY_MADE and CUSTOM_AVAILABLE products.
-- product_id is both PK and FK → product.id (1:1 extension of product, ERD §11).
-- PORTFOLIO_ONLY products do not require an inventory row.
-- CHECK quantity_on_hand >= 0: database-level guarantee against negative stock
-- (FR-INV-01, BR-15) — this is the second line of defence behind application logic.
-- No version/locking column: DEC-009 (concurrency strategy) remains OPEN.
-- -----------------------------------------------------------------------------
CREATE TABLE inventory (
    product_id        BIGINT      NOT NULL
                                  REFERENCES product (id)
                                  ON DELETE CASCADE,
    quantity_on_hand  INTEGER     NOT NULL DEFAULT 0
                                  CHECK (quantity_on_hand >= 0),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_inventory PRIMARY KEY (product_id)
);

-- =============================================================================
-- End of V3__create_catalogue_inventory_tables.sql
-- =============================================================================
