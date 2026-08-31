# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 3 — Backend Functional Development

## Current Module

Phase 3A.1 — Authentication Foundation (Register, Login, JWT) — COMPLETED

## Last Verified Milestone

Phase 3A.1 — Authentication Foundation — COMPLETED and VERIFIED.

`mvn clean test`
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway, DataSource, migration baseline (Phase 2A).

Identity and Address persistence model implemented (Phase 2B). Verified against live PostgreSQL.

Catalogue and Inventory persistence model implemented and fully verified against live PostgreSQL: Category, Product, ProductImage, ProductRelated, Inventory entities, repositories, V3 migration (Phase 2C). All 36 db-integration tests pass.

Commerce persistence model implemented and fully verified against live PostgreSQL: Cart, CartItem, CustomerOrder, OrderItem, Payment entities, repositories, V4 migration (Phase 2D). All 53 db-integration tests pass.

Phase 3A.1 — Authentication Foundation implemented: Spring Security (JWT-based stateless), BCrypt password hashing, customer registration, login, JWT generation/validation, authenticated-user resolution, error handling. Temporary DevSecurityConfig replaced. All 22 tests pass (default profile, no PostgreSQL required): 13 controller MockMvc tests + 8 AuthService unit tests + 1 contextLoads.

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
* [-] Authentication and Authorization (Phase 3A.1 complete — Phase 3A.2 not started)
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
Enum stubs present (not yet implemented): CustomOrderRequestStatus, QuotationStatus, ShipmentStatus

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
- Payment (entity: payment table)

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

Phase 3A.1 authentication components:
- Security: DevSecurityConfig replaced by SecurityConfig (JWT-stateless, CSRF disabled, public routes: /register + /login)
- Security: AppUserDetailsService (loads AppUser by email, case-insensitive)
- Security: JwtService (HMAC-SHA256, externalized secret + expiry via app.jwt.secret / app.jwt.expiration-ms)
- Security: JwtAuthenticationFilter (Bearer token extraction and validation per request)
- Security: AuthEntryPoint (structured JSON 401 for unauthenticated requests)
- Service: AuthService (register → CUSTOMER only; login → BadCredentialsException on failure; getCurrentUser)
- Controller: AuthController (POST /api/v1/auth/register, POST /api/v1/auth/login, GET /api/v1/auth/me)
- DTOs: RegisterRequest, LoginRequest, UserResponse, LoginResponse (password_hash never included)
- Exceptions: ApiError (error envelope per SDD §12.2), DuplicateEmailException, GlobalExceptionHandler
- Config: app.jwt.secret and app.jwt.expiration-ms externalized; no hardcoded secrets

Build verification: mvn clean test — PASSED. 14 tests, 0 failures, 0 errors (default profile). BUILD SUCCESS.

## Frontend Status

Not started.

## Database Status

PostgreSQL: DataSource configured, driver present, Flyway configured.

Runtime DataSource auto-configuration: ENABLED.

Flyway: V1–V4 migration scripts present and applied. V5 not yet created.

Hibernate ddl-auto: none (Flyway owns schema).

PostgreSQL connectivity: VERIFIED.

Flyway V1 applied: VERIFIED.
Flyway V2 applied: VERIFIED.
Flyway V3 applied: VERIFIED — confirmed by developer during Phase 2C live integration run.
Flyway V4 applied: VERIFIED — confirmed by developer during Phase 2D live integration run.

JPA persistence (AppUser, Address): VERIFIED via db-integration tests.
JPA persistence (Category, Product, ProductImage, ProductRelated, Inventory): VERIFIED — developer ran full db-integration suite against live PostgreSQL. Tests run: 36, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
JPA persistence (Cart, CartItem, CustomerOrder, OrderItem, Payment): VERIFIED — developer ran full db-integration suite against live PostgreSQL. All Phase 2A–2D tests pass. Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Database integration tests: Tagged @Tag("db-integration"). Run with:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  (environment variables: DB_URL, DB_USERNAME, DB_PASSWORD)

## API Status

REST API contract approved.

Implemented (Phase 3A.1):
- POST /api/v1/auth/register — 201 + UserResponse; 400 validation; 409 duplicate email
- POST /api/v1/auth/login   — 200 + LoginResponse (access token + user summary); 400; 401
- GET  /api/v1/auth/me      — 200 + UserResponse; 401 unauthenticated

## Testing Status

Test strategy approved.

Tests implemented: 6 classes
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile)
- AuthControllerTest (default profile — Phase 3A.1: 13 tests)
- DatabaseInfrastructureIntegrationTest (db-integration — Phase 2A: 4 tests)
- IdentityPersistenceIntegrationTest (db-integration — Phase 2B: 13 tests)
- CatalogueInventoryPersistenceIntegrationTest (db-integration — Phase 2C: 19 tests)
- CommercePersistenceIntegrationTest (db-integration — Phase 2D: 17 tests)

Tests executed (default profile): 14 (Phase 3A.1 verification run)

Tests passed (default profile): 14

Tests failed (default profile): 0

Database integration tests (Phase 2A–2D): EXECUTED and PASSED (prior phase verification).
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS. All Phase 2A–2D database integration tests pass.

## Current Known Issues

No blocking issues for Phase 3A.2.

DEC-009 (inventory concurrency strategy): OPEN — no locking column added yet. Will be resolved in the appropriate transactional implementation phase.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED.
DEC-010 (default address behavior): DEFERRED.
DEC-009 (inventory concurrency strategy): OPEN — does not block Phase 2C persistence.
DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-005 (advance payment rule), DEC-006 (order cancellation eligibility), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN.

## Last Completed Task

Phase 3A.1 — Authentication Foundation — COMPLETED and VERIFIED (test-quality review passed).

Build verification: `mvn clean test`
Result: Tests run: 22, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/config/SecurityConfig.java
- backend/src/main/java/com/handmadeart/ecommerce/security/JwtService.java
- backend/src/main/java/com/handmadeart/ecommerce/security/AppUserDetailsService.java
- backend/src/main/java/com/handmadeart/ecommerce/security/JwtAuthenticationFilter.java
- backend/src/main/java/com/handmadeart/ecommerce/security/AuthEntryPoint.java
- backend/src/main/java/com/handmadeart/ecommerce/service/AuthService.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/AuthController.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/auth/RegisterRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/auth/LoginRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/auth/UserResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/auth/LoginResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/ApiError.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/DuplicateEmailException.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/GlobalExceptionHandler.java
- backend/src/test/java/com/handmadeart/ecommerce/AuthControllerTest.java (13 tests)
- backend/src/test/java/com/handmadeart/ecommerce/AuthServiceTest.java (8 tests)

Files modified:
- backend/pom.xml (added JJWT 0.12.6)
- backend/src/main/resources/application.yml (added app.jwt.secret + app.jwt.expiration-ms)
- backend/src/test/resources/application.yml (added test JWT properties)
- backend/src/test/java/com/handmadeart/ecommerce/HandmadeArtEcommerceApplicationTests.java
- project-docs/DEVELOPMENT_STATUS.md

Files deleted:
- backend/src/main/java/com/handmadeart/ecommerce/config/DevSecurityConfig.java

## Prior Last Completed Task (Phase 2D)

Phase 2D — Commerce Database Model — COMPLETED and VERIFIED.

Live database verification: `mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration`
Result: Tests run: 53, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Flyway V4 migration applied and verified against live PostgreSQL.
All Phase 2A–2D database integration tests pass.

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

None. Phase 3A.1 complete. Awaiting Phase 3A.2 prompt.

## Next Recommended Task

Phase 3A.2 — Full Endpoint Authorization.

Apply role-based and ownership-based access control to all implemented endpoints as modules are added. Configure ADMIN-only and CUSTOMER-only route rules in SecurityConfig. Implement method-level security where required. DEC-002 (JWT logout/revocation) remains OPEN — resolve before implementing logout.
