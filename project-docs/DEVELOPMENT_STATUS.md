# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 2 — Database Foundation

## Current Module

Phase 2E — Custom Artwork Database Model — Not Started

## Last Verified Milestone

Phase 2D — Commerce Database Model — COMPLETED and VERIFIED against live PostgreSQL.

`mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway, DataSource, migration baseline (Phase 2A).

Identity and Address persistence model implemented (Phase 2B). Verified against live PostgreSQL.

Catalogue and Inventory persistence model implemented and fully verified against live PostgreSQL: Category, Product, ProductImage, ProductRelated, Inventory entities, repositories, V3 migration (Phase 2C). All 36 db-integration tests pass.

Commerce persistence model implemented and fully verified against live PostgreSQL: Cart, CartItem, CustomerOrder, OrderItem, Payment entities, repositories, V4 migration (Phase 2D). All 53 db-integration tests pass.

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
* [x] Database Foundation (Phase 2A–2C complete and verified; Phase 2D–2F pending)
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
* [ ] Phase 2E — Custom Artwork Database Model
* [ ] Phase 2F — Database Integration Validation

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

Enums created: UserRole, CategoryStatus, ProductStatus, ProductType, OrderStatus, PaymentStatus, PaymentPurpose

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
- PaymentRepository (findByOrderId, findByCustomOrderRequestId, findByOrderIdAndStatus, findByProviderTransactionReference)

Temporary dev security configuration in place (Phase 1 only — to be replaced in Phase 3).

Build verification: mvn clean test — PASSED. 1 test, 0 failures, 0 errors (default profile). Phase 2D: 34 source files, 5 test files compiled. BUILD SUCCESS.

Live DB integration verification (Phase 2D): mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration — PASSED. Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Frontend Status

Not started.

## Database Status

PostgreSQL: DataSource configured, driver present, Flyway configured.

Runtime DataSource auto-configuration: ENABLED.

Flyway: V1, V2, V3 migration scripts present and applied.

Hibernate ddl-auto: none (Flyway owns schema).

PostgreSQL connectivity: VERIFIED.

Flyway V1 applied: VERIFIED.
Flyway V2 applied: VERIFIED.
Flyway V3 applied: VERIFIED — confirmed by developer during Phase 2C live integration run.
Flyway V4 applied: VERIFIED — confirmed by developer during Phase 2D live integration run.

JPA persistence (AppUser, Address): VERIFIED via db-integration tests.
JPA persistence (Category, Product, ProductImage, ProductRelated, Inventory): VERIFIED — developer ran full db-integration suite against live PostgreSQL. Tests run: 36, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
JPA persistence (Cart, CartItem, CustomerOrder, OrderItem, Payment): VERIFIED — developer ran full db-integration suite against live PostgreSQL. All Phase 2A–2D tests pass. BUILD SUCCESS.

Database integration tests: Tagged @Tag("db-integration"). Run with:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  (environment variables: DB_URL, DB_USERNAME, DB_PASSWORD)

## API Status

REST API contract approved.

Implementation not started.

## Testing Status

Test strategy approved.

Tests implemented: 5 classes
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile)
- DatabaseInfrastructureIntegrationTest (db-integration — Phase 2A: 4 tests)
- IdentityPersistenceIntegrationTest (db-integration — Phase 2B: 13 tests)
- CatalogueInventoryPersistenceIntegrationTest (db-integration — Phase 2C: 19 tests)
- CommercePersistenceIntegrationTest (db-integration — Phase 2D: 17 tests)

Tests executed (default profile): 1 (HandmadeArtEcommerceApplicationTests.contextLoads)

Tests passed (default profile): 1

Tests failed (default profile): 0

Database integration tests (Phase 2A + 2B + 2C + 2D): EXECUTED and PASSED.
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Current Known Issues

No blocking issues for Phase 2E.

DEC-009 (inventory concurrency strategy): OPEN — no locking column added yet. Will be resolved in the appropriate transactional implementation phase.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED.
DEC-010 (default address behavior): DEFERRED.
DEC-009 (inventory concurrency strategy): OPEN — does not block Phase 2C persistence.
DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-005 (advance payment rule), DEC-006 (order cancellation eligibility), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN.

## Last Completed Task

Phase 2D — Commerce Database Model — COMPLETED and VERIFIED.

Live database verification: `mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Result: Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Flyway V4 migration applied and verified against live PostgreSQL.
All Phase 2A–2D database integration tests pass.

---

Phase 2D — Commerce Database Model — Implementation summary:

Enums created: OrderStatus, PaymentStatus, PaymentPurpose.

Entities created: Cart, CartItem, CustomerOrder, OrderItem, Payment.

Repositories created: CartRepository, CartItemRepository, CustomerOrderRepository, OrderItemRepository, PaymentRepository.

Migration created: V4__create_commerce_tables.sql (cart, cart_item, customer_order, order_item, payment tables with all approved constraints, FKs, indexes).

Note: payment.custom_order_request_id FK to custom_order_request is deferred to V5 (Phase 2E) because the custom_order_request table does not yet exist. Column is created in V4 without FK constraint; FK added in V5.

Tests added: CommercePersistenceIntegrationTest — 17 db-integration tests.

Build: mvn clean test — PASSED. 34 source files, 5 test files, 1 test executed, 0 failures. BUILD SUCCESS.

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/entity/OrderStatus.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/PaymentStatus.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/PaymentPurpose.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Cart.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/CartItem.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/CustomerOrder.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/OrderItem.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Payment.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CartRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CartItemRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/CustomerOrderRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/OrderItemRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/PaymentRepository.java
- backend/src/main/resources/db/migration/V4__create_commerce_tables.sql
- backend/src/test/java/com/handmadeart/ecommerce/CommercePersistenceIntegrationTest.java

Files modified:
- project-docs/DEVELOPMENT_STATUS.md

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

None. Phase 2D verification complete. Awaiting Phase 2E prompt.

## Next Recommended Task

Phase 2E — Custom Artwork Database Model.

Define JPA entities for CustomOrderRequest, CustomOrderImage, Quotation, and Shipment per the approved Database Design & ERD (Sections 3.13–3.16). Create Flyway migration V5. V5 must also add the deferred FK: payment.custom_order_request_id → custom_order_request(id).
