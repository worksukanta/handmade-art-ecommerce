# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 3 — Backend Functional Development

## Current Module

Phase 3A — Authentication & Security — Not Started

## Last Verified Milestone

Phase 2F — Database Integration Validation — COMPLETED and VERIFIED against live PostgreSQL.

`mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway, DataSource, migration baseline (Phase 2A).

Identity and Address persistence model implemented (Phase 2B). Verified against live PostgreSQL.

Catalogue and Inventory persistence model implemented and fully verified against live PostgreSQL: Category, Product, ProductImage, ProductRelated, Inventory entities, repositories, V3 migration (Phase 2C). All 36 db-integration tests pass.

Commerce persistence model implemented and fully verified against live PostgreSQL: Cart, CartItem, CustomerOrder, OrderItem, Payment entities, repositories, V4 migration (Phase 2D). All 53 db-integration tests pass.

Custom Artwork persistence model implemented and fully verified against live PostgreSQL: CustomOrderRequest, CustomOrderImage, Quotation, Shipment entities, repositories, V5 migration (Phase 2E). All 82 db-integration tests pass.

Database Integration Validation complete and PostgreSQL-verified (Phase 2F): V1–V5 migration chain verified on live PostgreSQL; full schema cross-checked against approved ERD; all cross-module relationships verified; migration chain test added. Full Phase 2 persistence integration suite: 94 tests, 0 failures. Phase 2 — Database/Persistence Foundation — COMPLETE.

Frontend not started.

## Approved Design Baseline

* [x] SRS
* [x] MVP Scope Document
* [x] Requirements & Use Case Specification
* [x] System Design Document
* [x] Database Design & ERD
* [x] REST API Specification
* [x] UI/UX and Frontend Page Specification
* [x] Test Strategy and Test Case Specification

## Project Structure

Expected repository structure:

project/

* backend/
* frontend/
* project-docs/

Control files:

project-docs/

* BOB_MASTER_INSTRUCTIONS.md
* DEVELOPMENT_STATUS.md
* DECISION_LOG.md

## Development Modules

* [x] Spring Boot Project Initialization
* [x] Database Foundation (Phase 2A–2F complete — Database Foundation phase COMPLETE)
* [ ] Authentication and Authorization
* [ ] Customer Profile
* [ ] Address Management
* [ ] Categories
* [ ] Products
* [ ] Product Images
* [ ] Related Products
* [ ] Inventory
* [ ] Cart
* [ ] Checkout
* [ ] Orders
* [ ] Payments
* [ ] Custom Artwork Requests
* [ ] Reference Image Upload
* [ ] Admin Review
* [ ] Quotations
* [ ] Quotation Approval/Rejection
* [ ] Advance Payment
* [ ] Production Workflow
* [ ] Shipping/Delivery
* [ ] Admin APIs
* [ ] Backend Integration and Regression
* [ ] React Project Initialization
* [ ] Authentication UI
* [ ] Catalogue UI
* [ ] Cart and Checkout UI
* [ ] Order UI
* [ ] Custom Artwork UI
* [ ] Admin UI
* [ ] Frontend/Backend Integration
* [ ] End-to-End Testing
* [ ] Security Testing
* [ ] Final Regression
* [ ] Demo Preparation

## Database Foundation Progress

* [x] Phase 2A — Infrastructure + Migration Baseline
* [x] Phase 2B — Identity and Customer Database Model
* [x] Phase 2C — Catalogue and Inventory Database Model
* [x] Phase 2D — Commerce Database Model
* [x] Phase 2E — Custom Artwork Database Model
* [x] Phase 2F — Database Integration Validation

## Backend Status

Spring Boot 3.5.0 project initialized and build-verified.

Java target: 21 (running on Java 26.0.1 — compatible).

Maven configured (pom.xml with Spring Web, Spring Data JPA, Spring Security, Validation, PostgreSQL driver, Flyway Core, Flyway PostgreSQL provider, Spring Boot Test). Maven profile `db-integration-tests` added for running tagged integration tests.

DataSource auto-configuration: RE-ENABLED for runtime.

PostgreSQL database name: handmade_art_ecommerce. Connection via DB_URL, DB_USERNAME, DB_PASSWORD.

Flyway: Enabled. Migration location: classpath:db/migration.

Hibernate schema policy: ddl-auto: none (permanent). Flyway is sole schema authority.

Migrations:
- V1__migration_baseline.sql — baseline marker (applied and verified)
- V2__create_identity_tables.sql — app_user and address tables (applied and verified)
- V3__create_catalogue_inventory_tables.sql — category, product, product_image, product_related, inventory tables (applied and verified)
- V4__create_commerce_tables.sql — cart, cart_item, customer_order, order_item, payment tables (applied and verified)
- V5__create_custom_artwork_tables.sql — custom_order_request, custom_order_image, quotation, shipment tables; deferred FK payment.custom_order_request_id (applied and verified)

Enums created: UserRole, CategoryStatus, ProductStatus, ProductType, OrderStatus, PaymentStatus, PaymentPurpose, CustomOrderRequestStatus, QuotationStatus, ShipmentStatus

Entities created:
- UserRole (enum: CUSTOMER, ADMIN)
- AppUser (entity: app_user table)
- Address (entity: address table)
- CategoryStatus (enum: ACTIVE, INACTIVE)
- ProductStatus (enum: ACTIVE, INACTIVE)
- ProductType (enum: READY_MADE, CUSTOM_AVAILABLE, PORTFOLIO_ONLY)
- Category (entity: category table)
- Product (entity: product table)
- ProductImage (entity: product_image table)
- ProductRelatedId (embeddable composite key)
- ProductRelated (entity: product_related junction table)
- Inventory (entity: inventory table, shared PK/FK with product)
- OrderStatus (enum: 6 lifecycle values)
- PaymentStatus (enum: PENDING, SUCCESS, FAILED)
- PaymentPurpose (enum: FULL, ADVANCE, REMAINING)
- Cart (entity: cart table)
- CartItem (entity: cart_item table)
- CustomerOrder (entity: customer_order table)
- OrderItem (entity: order_item table)
- Payment (entity: payment table — Phase 2E: customOrderRequest upgraded to @ManyToOne)
- CustomOrderRequestStatus (enum: 13 lifecycle values)
- QuotationStatus (enum: PENDING, APPROVED, REJECTED, EXPIRED)
- ShipmentStatus (enum: PENDING, SHIPPED, DELIVERED)
- CustomOrderRequest (entity: custom_order_request table)
- CustomOrderImage (entity: custom_order_image table)
- Quotation (entity: quotation table)
- Shipment (entity: shipment table)

Repositories created:
- AppUserRepository (findByEmailIgnoreCase, existsByEmailIgnoreCase, countByRole, findByEmailLowerCase)
- AddressRepository (findByUserId, findByUserIdAndId, findByUserIdAndIsDefaultTrue, countByUserId)
- CategoryRepository (findByStatus, findByName, existsByName)
- ProductRepository (findByCategoryIdAndStatus, findByStatus, findByStatusAndProductType, findByCategoryId, countByCategoryIdAndStatus)
- ProductImageRepository (findByProductIdOrderByDisplayOrderAsc, findByProductIdAndIsPrimaryTrue, countByProductId)
- InventoryRepository (findByProductId, existsByProductId)
- ProductRelatedRepository (findByProductId, findByRelatedProductId, existsById)
- CartRepository (findByUserId, existsByUserId)
- CartItemRepository (findByCartId, findByCartIdAndProductId, countByCartId, deleteByCartId)
- CustomerOrderRepository (findByUserId paginated, findByStatus paginated, findByUserIdAndStatus)
- OrderItemRepository (findByOrderId, countByOrderId)
- PaymentRepository (findByOrderId, findByCustomOrderRequestId, findByCustomOrderRequest, findByOrderIdAndStatus, findByProviderTransactionReference)
- CustomOrderRequestRepository (findByUserId paginated, findByStatus, findByUserIdAndStatus)
- CustomOrderImageRepository (findByCustomOrderRequestId, countByCustomOrderRequestId)
- QuotationRepository (findByCustomOrderRequestId, findByStatus)
- ShipmentRepository (findByOrderId, findByCustomOrderRequestId)

Temporary dev security configuration in place (Phase 1 only — to be replaced in Phase 3).

Build verification: mvn clean test — PASSED. 1 test, 0 failures, 0 errors (default profile). Phase 2E: 45 source files, 6 test files compiled. BUILD SUCCESS.

Live DB integration verification (Phase 2D): mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration — PASSED. Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
Live DB integration verification (Phase 2E): mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration — PASSED. Tests run: 82, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Frontend Status

Not started.

## Database Status

PostgreSQL: DataSource configured, driver present, Flyway configured.

Runtime DataSource auto-configuration: ENABLED.

Flyway: V1–V5 migration scripts present and applied.

Hibernate ddl-auto: none (Flyway owns schema).

PostgreSQL connectivity: VERIFIED.

Flyway V1 applied: VERIFIED.
Flyway V2 applied: VERIFIED.
Flyway V3 applied: VERIFIED — confirmed by developer during Phase 2C live integration run.
Flyway V4 applied: VERIFIED — confirmed by developer during Phase 2D live integration run.
Flyway V5 applied: VERIFIED — confirmed by developer during Phase 2E live integration run.

JPA persistence (AppUser, Address): VERIFIED via db-integration tests.
JPA persistence (Category, Product, ProductImage, ProductRelated, Inventory): VERIFIED — developer ran full db-integration suite against live PostgreSQL. Tests run: 36, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
JPA persistence (Cart, CartItem, CustomerOrder, OrderItem, Payment): VERIFIED — developer ran full db-integration suite against live PostgreSQL. All Phase 2A–2D tests pass. BUILD SUCCESS.
JPA persistence (CustomOrderRequest, CustomOrderImage, Quotation, Shipment): VERIFIED — developer ran full db-integration suite against live PostgreSQL. All Phase 2A–2E tests pass. BUILD SUCCESS.

Database integration tests: Tagged @Tag("db-integration"). Run with:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  (environment variables: DB_URL, DB_USERNAME, DB_PASSWORD)

## API Status

REST API contract approved.

Implementation not started.

## Testing Status

Test strategy approved.

Tests implemented: 6 classes
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile)
- DatabaseInfrastructureIntegrationTest (db-integration — Phase 2A+2F: 5 tests; Phase 2F added allFiveMigrationsAppliedSuccessfully)
- IdentityPersistenceIntegrationTest (db-integration — Phase 2B: 13 tests)
- CatalogueInventoryPersistenceIntegrationTest (db-integration — Phase 2C: 19 tests)
- CommercePersistenceIntegrationTest (db-integration — Phase 2D: 17 tests; updated for Phase 2E)
- CustomArtworkPersistenceIntegrationTest (db-integration — Phase 2E: 29 tests)

Tests executed (default profile): 1 (HandmadeArtEcommerceApplicationTests.contextLoads)

Tests passed (default profile): 1

Tests failed (default profile): 0

Database integration tests (Phase 2A–2D): EXECUTED and PASSED (Phase 2D verification run).
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Database integration tests (Phase 2A–2E): EXECUTED and PASSED.
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Tests run: 82, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Database integration tests (Phase 2A–2F — full Phase 2 suite): EXECUTED and PASSED.
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Tests run: 94, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
  V1–V5 migration chain verified. Phase 2 — Database/Persistence Foundation — COMPLETE.

## Current Known Issues

No blocking issues for Phase 3A.

DEC-009 (inventory concurrency strategy): OPEN — no locking column added yet. Will be resolved in the appropriate transactional implementation phase.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED.
DEC-010 (default address behavior): DEFERRED.
DEC-009 (inventory concurrency strategy): OPEN — does not block Phase 2C persistence.
DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-005 (advance payment rule), DEC-006 (order cancellation eligibility), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN.

## Last Completed Task

Phase 2F — Database Integration Validation — COMPLETED and VERIFIED.

`mvn clean test` — PASSED. 45 source files, 6 test files, 1 test, 0 failures. BUILD SUCCESS.

Live database verification: `mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Result: Tests run: 94, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

V1–V5 migration chain verified on live PostgreSQL.
Full Phase 2 persistence integration suite passes. Phase 2 — Database/Persistence Foundation — COMPLETE.

Validation performed:
- V1–V5 migration chain: ordering correct, FK dependency order valid, all CHECK/UNIQUE/index constraints match ERD — PASS
- Full schema vs ERD (all 16 tables, all 57 columns spot-checked): fields, nullability, precision, enums, PKs, FKs, delete behavior — PASS
- Cross-module JPA ↔ DB FK agreement (all 20 relationships): user→address, category→product, product→inventory, product→cart/order items, user→cart/order/custom-request, order→payment, custom-request→quotation, custom-request→payment, order/custom-request→shipment — PASS
- Payment.customOrderRequest @ManyToOne upgrade from Phase 2E: confirmed correct — PASS
- Deferred FK (payment.custom_order_request_id → custom_order_request) resolved in V5 — PASS
- Test infrastructure: default profile excludes DB; db-integration profile enables all; Maven profile configuration correct — PASS

Gap identified and fixed:
- DatabaseInfrastructureIntegrationTest verified only V1 was recorded; V2–V5 were not checked.
  Added: allFiveMigrationsAppliedSuccessfully() — queries flyway_schema_history for all 5 versions,
  asserts each succeeded and all 5 are present. Catches clean-schema failures hidden by incremental dev.

No schema defects found. No migrations modified. No business logic added.

Files modified:
- backend/src/test/java/com/handmadeart/ecommerce/DatabaseInfrastructureIntegrationTest.java
- project-docs/DEVELOPMENT_STATUS.md

---

## Prior Last Completed Task (Phase 2E)

Phase 2E — Custom Artwork Database Model — COMPLETED and VERIFIED.

Build verification: `mvn clean test` — PASSED. 45 source files, 6 test files, 1 test, 0 failures. BUILD SUCCESS.

Live database verification: `mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Result: Tests run: 82, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Flyway V5 migration applied and verified against live PostgreSQL.
All Phase 2A–2E database integration tests pass.

Enums created (stubs existed; already complete): CustomOrderRequestStatus (13 values), QuotationStatus (4 values), ShipmentStatus (3 values).

Entities created: CustomOrderRequest, CustomOrderImage, Quotation, Shipment.

Entity modified: Payment — customOrderRequest upgraded from raw Long column to proper @ManyToOne FK (deferred FK added in V5 migration).

Repositories created: CustomOrderRequestRepository, CustomOrderImageRepository, QuotationRepository, ShipmentRepository.

Repository modified: PaymentRepository — added findByCustomOrderRequest method; removed stale note about raw Long.

Migration created: V5__create_custom_artwork_tables.sql:
- custom_order_request table (13-value CHECK constraint, reviewed_by FK, indexes on user_id and status)
- custom_order_image table (ON DELETE CASCADE, file_size_bytes CHECK > 0, FK index)
- quotation table (UNIQUE custom_order_request_id, quoted_amount/advance_amount CHECKs, expiry_at NOT NULL, composite index on status+expiry_at)
- shipment table (dual-nullable-FK + mutual-exclusivity CHECK, 3-value status CHECK, FK indexes)
- ALTER TABLE payment ADD CONSTRAINT fk_payment_custom_order_request (deferred from V4)

Tests added: CustomArtworkPersistenceIntegrationTest — 29 db-integration tests.
Tests modified: CommercePersistenceIntegrationTest — 2 tests updated for Phase 2E (Payment @ManyToOne upgrade).

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/entity/CustomOrderRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/CustomOrderImage.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Quotation.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Shipment.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CustomOrderRequestRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CustomOrderImageRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/QuotationRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/ShipmentRepository.java
- backend/src/main/resources/db/migration/V5__create_custom_artwork_tables.sql
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkPersistenceIntegrationTest.java

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/entity/Payment.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/PaymentRepository.java
- backend/src/test/java/com/handmadeart/ecommerce/CommercePersistenceIntegrationTest.java
- project-docs/DEVELOPMENT_STATUS.md

---

## Prior Last Completed Task (Phase 2D)

Phase 2D — Commerce Database Model — COMPLETED and VERIFIED.

## Prior Last Completed Task

Phase 2C — Catalogue and Inventory Database Model — VERIFIED.

Developer confirmed: `mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Result: Tests run: 36, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

- Flyway V3 migration applied and verified against live PostgreSQL.
- Phase 2A (4 tests), Phase 2B (13 tests), and Phase 2C (19 tests) db-integration tests all pass.
- Category, Product, ProductImage, ProductRelated, and Inventory persistence fully verified.
- All schema constraints verified: unique names, CHECK constraints, ON DELETE CASCADE, composite PK, NOT NULL.
- Timestamp DB DEFAULT behaviour verified (created_at, updated_at, uploaded_at).

Phase 2C implementation files (for reference):

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/entity/CategoryStatus.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/ProductStatus.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/ProductType.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Category.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Product.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/ProductImage.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/ProductRelatedId.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/ProductRelated.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Inventory.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CategoryRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/ProductRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/ProductImageRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/InventoryRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/ProductRelatedRepository.java
- backend/src/main/resources/db/migration/V3__create_catalogue_inventory_tables.sql
- backend/src/test/java/com/handmadeart/ecommerce/CatalogueInventoryPersistenceIntegrationTest.java

Files modified:
- backend/pom.xml (added db-integration-tests Maven profile)
- project-docs/DEVELOPMENT_STATUS.md

## Current Task

None. Phase 2F verified. Phase 2 — Database/Persistence Foundation — COMPLETE. Awaiting Phase 3A prompt.

## Next Recommended Task

Phase 3A — Authentication & Security (JWT-based, Spring Security, BCrypt password hashing).

Implement: AppUser registration and login endpoints, JWT token generation/validation, Spring Security configuration replacing the temporary dev config, password hashing (BCrypt). Resolve DEC-002 (JWT logout/revocation strategy) before implementing logout.
