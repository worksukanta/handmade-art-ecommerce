# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 3 — Backend Functional Development

## Current Module

Phase 3E — Custom Artwork Workflow — COMPLETED
Phase 3 — Backend Functional Development — remaining admin/customer API gap review and backend final validation next

## Last Verified Milestone

Phase 3E.2 — Quotation Approval/Rejection, Advance Payment, Admin Production Workflow, Shipping — COMPLETED and VERIFIED.

`mvn clean test` (default profile, no PostgreSQL required)
Result: Tests run: 270, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

DEC-001 (payment provider): DEFERRED — sandbox/mock flow implemented (immediate SUCCESS on initiation).
DEC-002 (JWT logout/revocation) remains OPEN. Logout not implemented.
DEC-003 (file upload type/size limits) remains OPEN. Image upload implemented with content-type validation (image/* only); exact size limits not enforced pending DEC-003 resolution.
DEC-004 (requotation): DEFERRED — one quotation per request enforced; re-quotation not implemented.
DEC-005 (advance payment rule): APPROVED and IMPLEMENTED — authoritative amount = stored Quotation.advanceAmount; no client-supplied amount accepted.
DEC-008 (shipping/tracking): APPROVED and IMPLEMENTED — status/tracking-based; no carrier API integration.
DEC-009 (inventory concurrency strategy): APPROVED and IMPLEMENTED — checkout-time pessimistic locking.
DEC-007 (tax/delivery charge) remains DEFERRED. Order totals = item subtotals only (totalAmount = subtotalAmount).
DEC-010 (default address) remains DEFERRED. Explicit owned addressId required; no silent fallback.
DEC-006 (order cancellation): OPEN — not in scope for Phase 3E.

PostgreSQL regression: NOT executed in this phase. Developer should run the full db-integration suite (V5 not yet verified against live DB).

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway, DataSource, migration baseline (Phase 2A).

Identity and Address persistence model implemented (Phase 2B). Verified against live PostgreSQL.

Catalogue and Inventory persistence model implemented and fully verified against live PostgreSQL: Category, Product, ProductImage, ProductRelated, Inventory entities, repositories, V3 migration (Phase 2C). All 36 db-integration tests pass.

Commerce persistence model implemented and fully verified against live PostgreSQL: Cart, CartItem, CustomerOrder, OrderItem, Payment entities, repositories, V4 migration (Phase 2D). All 53 db-integration tests pass.

Phase 3A.1 — Authentication Foundation implemented: Spring Security (JWT-based stateless), BCrypt password hashing, customer registration, login, JWT generation/validation, authenticated-user resolution, error handling. Temporary DevSecurityConfig replaced.

Phase 3A.2 — Authorization Foundation implemented: ApiAccessDeniedHandler (structured 403), CurrentUserService (ownership-resolution utility), SecurityAuthorizationTest (8 authorization tests).

Phase 3A.3 — Authentication & Security Integration Validation COMPLETED: defect fixed (DaoAuthenticationProvider deprecated no-arg constructor → DaoAuthenticationProvider(PasswordEncoder) constructor), CurrentUserService unit tests added (5 tests). All 35 tests pass (default profile, no PostgreSQL required): 13 controller + 8 AuthService + 5 CurrentUserService + 8 SecurityAuthorization + 1 contextLoads.

Phase 3B.1 — Public Catalogue APIs COMPLETED: category listing, category detail, product listing with search/filter/sort/pagination, product detail with images and availability and related products, related products list. Security config updated to permit catalogue public endpoints. ResourceNotFoundException + GlobalExceptionHandler handler added. All 54 tests pass.

Phase 3B.2 — Admin Catalogue Management APIs COMPLETED: POST/PUT/PATCH category management, POST/PUT/PATCH product management, admin product listing (all statuses), POST/DELETE product image metadata (multipart upload to local filesystem), PUT related-products (full replacement), GET/PATCH inventory management. DuplicateCategoryNameException added. GlobalExceptionHandler extended for 409 conflict types. All 94 tests pass.

Phase 3B.3 — Catalogue Integration Validation COMPLETED: defects fixed (broad IllegalStateException → 409 replaced with narrow InventoryTypeConflictException; double countByProductId() call in addProductImage() consolidated to single call); 3 high-value service tests added (PROD-08: READY_MADE→PORTFOLIO_ONLY removes inventory row; PROD-09: PORTFOLIO_ONLY→READY_MADE creates inventory row; IMG-01: removeProductImage with cross-product imageId → 404). All 97 tests pass (default profile, no PostgreSQL required). DEC-002 remains OPEN. Next phase: Phase 3C Cart APIs.

Phase 3E.1 — Custom Artwork Request + Admin Review + Quotation COMPLETED: customer create/list/get custom requests, reference image upload, admin list/review requests, admin create quotation, customer get quotation. Custom artwork DTOs, service, controller, admin controller. InvalidWorkflowTransitionException + DuplicateQuotationException added. GlobalExceptionHandler extended. SecurityConfig updated (/api/v1/custom-requests/** → CUSTOMER). All 225 tests pass.

Phase 3E.2 — Quotation Decision + Advance Payment + Admin Production Workflow + Shipping COMPLETED: customer approve/reject quotation (POST /quotations/{id}/approve|reject), advance payment (POST /custom-requests/{id}/payments, DEC-005), customer payment history (GET /custom-requests/{id}/payments), customer shipment view (GET /custom-requests/{id}/shipment), admin production status update (PATCH /admin/custom-requests/{id}/status), admin shipment management (POST/PATCH/GET /admin/shipments). CustomAdvancePaymentService, AdminProductionService created. ShipmentResponse, ShipmentCreateRequest, ShipmentStatusUpdateRequest DTOs created. SecurityConfig updated (/api/v1/quotations/** → CUSTOMER). All 270 tests pass (default profile, no PostgreSQL required). DEC-003 OPEN. DEC-004 DEFERRED. DEC-005 APPROVED and IMPLEMENTED. DEC-008 APPROVED and IMPLEMENTED.

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
* [x] Authentication and Authorization (Phase 3A COMPLETE)
* [ ] Customer Profile
* [ ] Address Management
* [x] Categories (Phase 3B.1: public read APIs COMPLETE; Phase 3B.2: admin CRUD COMPLETE)
* [x] Products (Phase 3B.1: public read APIs COMPLETE; Phase 3B.2: admin CRUD COMPLETE)
* [x] Product Images (Phase 3B.1: included in product detail response; Phase 3B.2: admin upload/remove COMPLETE)
* [x] Related Products (Phase 3B.1: related products endpoint COMPLETE; Phase 3B.2: admin manage COMPLETE)
* [x] Inventory (Phase 3B.2: admin GET/PATCH inventory COMPLETE — DEC-009 concurrency APPROVED: checkout-time pessimistic locking)
* [x] Cart (Phase 3C.1: customer cart APIs; Phase 3C.2: validation & ownership hardening; Phase 3C.3: integration validation — COMPLETE)
* [x] Checkout (Phase 3D.1: POST /api/v1/orders checkout flow with pessimistic locking — COMPLETE)
* [x] Orders (Phase 3D.1: order creation; Phase 3D.2: GET list + detail — COMPLETE)
* [x] Payments (Phase 3D.2: POST/GET /orders/{id}/payments — standard ready-made orders, DEC-001 sandbox — COMPLETE)
* [x] Custom Artwork Requests (Phase 3E.1: customer CRUD + admin list/review — COMPLETE)
* [x] Reference Image Upload (Phase 3E.1: multipart to filesystem, metadata to DB — COMPLETE)
* [x] Admin Review (Phase 3E.1: ACCEPT/REJECT with workflow transitions — COMPLETE)
* [x] Quotations (Phase 3E.1: admin create, customer read — COMPLETE)
* [x] Quotation Approval/Rejection (Phase 3E.2 — COMPLETE)
* [x] Advance Payment (Phase 3E.2 — COMPLETE, DEC-005)
* [x] Production Workflow (Phase 3E.2 — COMPLETE)
* [x] Shipping/Delivery (Phase 3E.2 — COMPLETE, DEC-008)
* [ ] Admin APIs (gap review pending)
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
* [x] Phase 2D — Commerce Database Model — COMPLETED and VERIFIED (V4 migration verified against live PostgreSQL; all Phase 2A–2D db-integration tests pass)
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
- V5__create_custom_artwork_tables.sql — custom_order_request, custom_order_image, quotation, shipment tables + deferred FK on payment (present, not yet run against live DB)

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

Phase 3A.2 authorization components:
- Security: ApiAccessDeniedHandler (structured JSON 403 for authenticated but unauthorized requests)
- Service: CurrentUserService (JWT-derived ownership resolution; reusable by all future customer-owned resource APIs)
- SecurityConfig updated: ApiAccessDeniedHandler wired; route rules: public (register/login), ADMIN (admin/**), authenticated (all others)

Phase 3A.3 validation changes:
- SecurityConfig fix: DaoAuthenticationProvider(PasswordEncoder) constructor used (replaces deprecated no-arg constructor)
- Test added: CurrentUserServiceTest (5 unit tests — CUS-01 through CUS-04b: authenticated user, email, null context, anonymous principal rejection)

Build verification: mvn clean test — PASSED. 35 tests, 0 failures, 0 errors (default profile). BUILD SUCCESS.

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
Flyway V4 applied: VERIFIED — confirmed by developer during Phase 2D live integration run (V4 migration verified against live PostgreSQL; all Phase 2A–2D db-integration tests pass).

JPA persistence (AppUser, Address): VERIFIED via db-integration tests.
JPA persistence (Category, Product, ProductImage, ProductRelated, Inventory): VERIFIED — developer ran full db-integration suite against live PostgreSQL. Tests run: 36, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.
JPA persistence (Cart, CartItem, CustomerOrder, OrderItem, Payment): VERIFIED — developer ran full db-integration suite against live PostgreSQL. All Phase 2A–2D tests pass. BUILD SUCCESS.
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Result: BUILD SUCCESS. All Phase 2A–2D database integration tests pass.

Database integration tests: Tagged @Tag("db-integration"). Run with:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  (environment variables: DB_URL, DB_USERNAME, DB_PASSWORD)

## API Status

REST API contract approved.

Implemented (Phase 3A.1):
- POST /api/v1/auth/register — 201 + UserResponse; 400 validation; 409 duplicate email
- POST /api/v1/auth/login   — 200 + LoginResponse (access token + user summary); 400; 401
- GET  /api/v1/auth/me      — 200 + UserResponse; 401 unauthenticated

Implemented (Phase 3B.1 — public catalogue, no auth required):
- GET /api/v1/categories              — 200 + CategoryResponse[] (ACTIVE only)
- GET /api/v1/categories/{id}         — 200 + CategoryResponse; 404 if INACTIVE/missing
- GET /api/v1/products                — 200 + PageResponse<ProductSummaryResponse>; q/categoryId/minPrice/maxPrice/sort/direction/page/size; 400 invalid sort
- GET /api/v1/products/{id}           — 200 + ProductDetailResponse (images, availability, related); 404 if INACTIVE/missing
- GET /api/v1/products/{id}/related-products — 200 + ProductSummaryResponse[]; 404 if source inactive/missing

Implemented (Phase 3B.2 — admin catalogue management, ADMIN role required):
- POST  /api/v1/admin/categories               — 201 + CategoryResponse; 400, 409 duplicate name
- PUT   /api/v1/admin/categories/{id}          — 200 + CategoryResponse; 400, 404, 409
- PATCH /api/v1/admin/categories/{id}/status   — 200 + CategoryResponse; 400, 404
- GET   /api/v1/admin/products                 — 200 + PageResponse<ProductSummaryResponse> (all statuses)
- POST  /api/v1/admin/products                 — 201 + ProductDetailResponse; 400, 404 category
- PUT   /api/v1/admin/products/{id}            — 200 + ProductDetailResponse; 400, 404
- PATCH /api/v1/admin/products/{id}/status     — 200 + ProductDetailResponse; 400, 404
- POST  /api/v1/admin/products/{id}/images     — 201 + ProductImageResponse; 400 (multipart, image/* only; DEC-003 size OPEN)
- DELETE /api/v1/admin/products/{id}/images/{imageId} — 204; 404
- PUT   /api/v1/admin/products/{id}/related-products — 200 + ProductSummaryResponse[]; 400 self-ref, 404
- GET   /api/v1/admin/inventory                — 200 + PageResponse<InventoryResponse>
- GET   /api/v1/admin/inventory/{productId}    — 200 + InventoryResponse; 404
- PATCH /api/v1/admin/inventory/{productId}    — 200 + InventoryResponse; 400 negative, 404, 409 PORTFOLIO_ONLY

Implemented (Phase 3C.1 — customer cart, CUSTOMER role required):
- GET    /api/v1/cart                          — 200 + CartResponse; lazy empty cart if none exists; 401
- POST   /api/v1/cart/items                    — 200 + CartResponse; lazy cart create; 400 invalid qty/type, 401, 404 product, 409 not-purchasable/stock
- PUT    /api/v1/cart/items/{itemId}           — 200 + CartResponse; 400 invalid qty, 401, 404 item/not-owned, 409 stock
- DELETE /api/v1/cart/items/{itemId}           — 200 + CartResponse; 401, 404 item/not-owned
- DELETE /api/v1/cart/items                    — 204 No Content; 401

Implemented (Phase 3D.1 — checkout/order creation, CUSTOMER role required):
- POST /api/v1/orders — 201 + OrderResponse; addressId required (owned); cart resolved server-side; pessimistic inventory lock/decrement; cart cleared; 400 missing addressId, 401, 403 ADMIN, 404 address not owned, 409 empty cart / insufficient stock / non-purchasable product

Implemented (Phase 3D.2 — order reads and standard payment, CUSTOMER role required):
- GET  /api/v1/orders                    — 200 + PageResponse<OrderSummaryResponse>; paginated (page, size); sorted newest first; CUSTOMER only
- GET  /api/v1/orders/{id}               — 200 + OrderResponse with item snapshots; ownership enforced (404 on foreign id); snapshot values only
- POST /api/v1/orders/{id}/payments      — 201 + PaymentResponse; amount from stored order total; DEC-001 sandbox: immediate SUCCESS + order → CONFIRMED; 400 missing method, 404 foreign order, 409 ORDER_NOT_PAYABLE
- GET  /api/v1/orders/{id}/payments      — 200 + PaymentResponse[]; ownership enforced (404 on foreign order)

Implemented (Phase 3E.1 — custom artwork request + admin review + quotation):
Customer (CUSTOMER role required):
- POST /api/v1/custom-requests              — 201 + CustomArtworkRequestResponse; initial status REQUESTED
- GET  /api/v1/custom-requests              — 200 + PageResponse<CustomArtworkRequestSummary>; page/size/status filter
- GET  /api/v1/custom-requests/{id}         — 200 + CustomArtworkRequestResponse; 404 on foreign/missing id
- POST /api/v1/custom-requests/{id}/images  — 201 + CustomOrderImageResponse; multipart image/*; DEC-003 size OPEN
- GET  /api/v1/custom-requests/{id}/quotation — 200 + QuotationResponse; 404 if not owned or no quotation
Admin (ADMIN role required):
- GET   /api/v1/admin/custom-requests                — 200 + PageResponse<CustomArtworkRequestSummary>; status filter
- PATCH /api/v1/admin/custom-requests/{id}/review    — 200 + CustomArtworkRequestResponse; ACCEPT/REJECT; 409 invalid transition
- POST  /api/v1/admin/custom-requests/{id}/quotation — 201 + QuotationResponse; requires UNDER_REVIEW; 409 duplicate/invalid
- GET   /api/v1/admin/quotations/{id}                — 200 + QuotationResponse

Implemented (Phase 3E.2 — quotation decision, advance payment, production, shipping):
Customer (CUSTOMER role required):
- POST /api/v1/quotations/{id}/approve          — 200 + QuotationResponse; PENDING→APPROVED; request QUOTED→APPROVED; expiry enforced; 404 foreign; 409 invalid state
- POST /api/v1/quotations/{id}/reject           — 200 + QuotationResponse; PENDING→REJECTED; request QUOTED→REJECTED; 404 foreign; 409 invalid state
- POST /api/v1/custom-requests/{id}/payments    — 201 + PaymentResponse; advance amount from Quotation.advanceAmount (DEC-005); APPROVED→IN_PRODUCTION (sandbox); 409 duplicate/wrong state
- GET  /api/v1/custom-requests/{id}/payments    — 200 + PaymentResponse[]; ownership enforced
- GET  /api/v1/custom-requests/{id}/shipment    — 200 + ShipmentResponse; ownership enforced; 404 no shipment
Admin (ADMIN role required):
- PATCH /api/v1/admin/custom-requests/{id}/status  — 200 + CustomArtworkRequestResponse; approved admin transitions only (IN_PRODUCTION→COMPLETED→SHIPPED→DELIVERED); 409 invalid
- POST  /api/v1/admin/shipments                    — 201 + ShipmentResponse; creates PENDING shipment for custom request or order; DEC-008
- PATCH /api/v1/admin/shipments/{id}/status        — 200 + ShipmentResponse; PENDING→SHIPPED→DELIVERED; sets shippedAt/deliveredAt; advances custom request status
- GET   /api/v1/admin/shipments/{id}               — 200 + ShipmentResponse

## Testing Status

Test strategy approved.

Tests implemented: 21 classes
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile)
- AuthControllerTest (default profile — Phase 3A.1: 13 tests)
- AuthServiceTest (default profile — Phase 3A.1: 8 tests)
- SecurityAuthorizationTest (default profile — Phase 3A.2: 8 tests)
- CurrentUserServiceTest (default profile — Phase 3A.3: 5 tests)
- CatalogueControllerTest (default profile — Phase 3B.1: 9 tests)
- CatalogueServiceTest (default profile — Phase 3B.1: 10 tests)
- AdminCatalogueControllerTest (default profile — Phase 3B.2: 19 tests)
- AdminCatalogueServiceTest (default profile — Phase 3B.2/3B.3: 24 tests — 3 added in 3B.3)
- CartControllerTest (default profile — Phase 3C.1/3C.2/3C.3: 20 tests)
- CartServiceTest (default profile — Phase 3C.1/3C.2: 24 tests)
- CheckoutControllerTest (default profile — Phase 3D.1: 8 tests: CHK-C-01..08)
- CheckoutServiceTest (default profile — Phase 3D.1: 12 tests: CHK-S-01..12)
- OrderControllerTest (default profile — Phase 3D.2: 6 tests: ORD-C-01..06)
- OrderServiceTest (default profile — Phase 3D.2: 6 tests: ORD-S-01..06)
- PaymentControllerTest (default profile — Phase 3D.2: 8 tests: PAY-C-01..08)
- PaymentServiceTest (default profile — Phase 3D.2: 9 tests: PAY-S-01..09)
- CustomArtworkControllerTest (default profile — Phase 3E.1: 15 tests: CAR-C-01..15)
- CustomArtworkServiceTest (default profile — Phase 3E.1: 20 tests: CAR-S-01..10, QUO-S-01..10)
- CustomArtworkPhase2ServiceTest (default profile — Phase 3E.2: 27 tests: QUO-S-11..18, ADV-S-01..11, PROD-S-01..08)
- CustomArtworkPhase2ControllerTest (default profile — Phase 3E.2: 18 tests: CAR2-C-01..18)
- DatabaseInfrastructureIntegrationTest (db-integration — Phase 2A: 4 tests)
- IdentityPersistenceIntegrationTest (db-integration — Phase 2B: 13 tests)
- CatalogueInventoryPersistenceIntegrationTest (db-integration — Phase 2C: 19 tests)
- CommercePersistenceIntegrationTest (db-integration — Phase 2D: 17 tests)

Tests executed (default profile): 270 (Phase 3E.2 verification run)

Tests passed (default profile): 270

Tests failed (default profile): 0

Database integration tests (Phase 2A–2D): EXECUTED and PASSED.
  Command: mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration
  Result: BUILD SUCCESS. All Phase 2A–2D database integration tests pass.

## Current Known Issues

No blocking issues for Phase 3E.

DEC-001 (payment provider): DEFERRED — sandbox mock flow (immediate SUCCESS). Real provider integration requires DEC-001 resolution.
DEC-003 (file upload type/size limits): OPEN — reference image upload implemented with content-type validation (image/* only). Exact file size limit not enforced until DEC-003 is resolved.
DEC-004 (requotation): DEFERRED — one quotation per request enforced; re-quotation not implemented.
DEC-005 (advance payment rule): APPROVED and IMPLEMENTED — authoritative amount = Quotation.advanceAmount; no client-supplied amount.
DEC-008 (shipping/tracking): APPROVED and IMPLEMENTED — status/tracking only; no carrier API integration.
DEC-009 (inventory concurrency strategy): APPROVED and IMPLEMENTED — checkout-time pessimistic locking via InventoryRepository.findByProductIdWithLock() (@Lock PESSIMISTIC_WRITE). Cart-time check remains advisory.
DEC-007 (tax/delivery charge): DEFERRED — order totalAmount = subtotalAmount (no tax/delivery).
DEC-010 (default address): DEFERRED — explicit owned addressId required at checkout; no silent default fallback.
DEC-006 (order cancellation): OPEN — not implemented; POST /orders/{id}/cancel is out of scope until DEC-006 is resolved.

V5 migration (custom artwork tables): present in classpath; not yet run against live PostgreSQL. Developer should run db-integration suite after Phase 3E commit.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED.
DEC-010 (default address behavior): DEFERRED.
DEC-009 (inventory concurrency strategy): APPROVED (checkout-time pessimistic locking) — did not block Phase 2C persistence.
DEC-005 (advance payment rule): APPROVED and IMPLEMENTED.
DEC-008 (shipping/tracking): APPROVED and IMPLEMENTED.
DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-006 (order cancellation eligibility), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN.

## Last Completed Task

Phase 3E.2 — Quotation Decision + Advance Payment + Admin Production Workflow + Shipping — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 270, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Endpoints implemented:
Customer (CUSTOMER role):
- POST /api/v1/quotations/{id}/approve          — 200 + QuotationResponse; PENDING→APPROVED; expiry enforced; 409 invalid state; 404 foreign
- POST /api/v1/quotations/{id}/reject           — 200 + QuotationResponse; PENDING→REJECTED; 409 invalid state; 404 foreign
- POST /api/v1/custom-requests/{id}/payments    — 201 + PaymentResponse; advance from Quotation.advanceAmount (DEC-005); sandbox→IN_PRODUCTION; 409 duplicate/wrong state
- GET  /api/v1/custom-requests/{id}/payments    — 200 + PaymentResponse[]; ownership enforced
- GET  /api/v1/custom-requests/{id}/shipment    — 200 + ShipmentResponse; ownership enforced; 404 no shipment
Admin (ADMIN role):
- PATCH /api/v1/admin/custom-requests/{id}/status  — 200 + CustomArtworkRequestResponse; IN_PRODUCTION→COMPLETED→SHIPPED→DELIVERED only; 409 invalid
- POST  /api/v1/admin/shipments                    — 201 + ShipmentResponse; PENDING initial status; DEC-008
- PATCH /api/v1/admin/shipments/{id}/status        — 200 + ShipmentResponse; PENDING→SHIPPED→DELIVERED; auto timestamps; advances custom request status
- GET   /api/v1/admin/shipments/{id}               — 200 + ShipmentResponse

Ownership enforcement:
- Customer user ID resolved exclusively from JWT (CurrentUserService) — no client-supplied IDs.
- Custom request ownership: resolveOwnedRequest() verifies userId match; foreign/missing id → 404 (non-disclosure).
- Quotation customer view: request ownership verified before quotation lookup.
- Admin endpoints: ADMIN role enforced by SecurityConfig (/api/v1/admin/**); CUSTOMER → 403.

Admin review workflow:
- ACCEPT: REQUESTED → UNDER_REVIEW; UNDER_REVIEW → UNDER_REVIEW (re-acknowledge + update notes).
- REJECT: REQUESTED or UNDER_REVIEW → REJECTED (terminal).
- Any other transition → InvalidWorkflowTransitionException (409 INVALID_TRANSITION).
- reviewedBy admin user recorded on request on every review action.

Quotation:
- Admin creates quotation only when request is UNDER_REVIEW.
- quotedAmount: BigDecimal >= 0; validated by @DecimalMin.
- advanceAmount: optional absolute value (DEC-005 OPEN — no fixed percentage).
- expiryAt: must be in the future (validated in service).
- Status on creation: PENDING. Request transitions UNDER_REVIEW → QUOTED atomically.
- One quotation per request enforced: duplicate → DuplicateQuotationException (409 DUPLICATE_QUOTATION).

Reference image handling:
- Multipart upload; content-type validated as image/*.
- UUID server-generated filename (prevents path traversal).
- Binary stored on filesystem (uploads/reference-images/request-{id}/); metadata only in PostgreSQL.
- storageReference is a logical path; raw filesystem root never exposed.
- DEC-003 OPEN: no size limit enforced.

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/CustomArtworkRequestCreateRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/CustomArtworkRequestResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/CustomArtworkRequestSummary.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/CustomOrderImageResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/QuotationResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/QuotationCreateRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/CustomRequestReviewRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/InvalidWorkflowTransitionException.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/DuplicateQuotationException.java
- backend/src/main/java/com/handmadeart/ecommerce/service/CustomArtworkRequestService.java
- backend/src/main/java/com/handmadeart/ecommerce/service/QuotationService.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/CustomArtworkController.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/AdminCustomArtworkController.java
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkServiceTest.java (20 tests: CAR-S-01..10, QUO-S-01..10)
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkControllerTest.java (15 tests: CAR-C-01..15)

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/config/SecurityConfig.java
  (/api/v1/custom-requests/** → CUSTOMER role added)
- backend/src/main/java/com/handmadeart/ecommerce/exception/GlobalExceptionHandler.java
  (InvalidWorkflowTransitionException → 409 INVALID_TRANSITION; DuplicateQuotationException → 409 DUPLICATE_QUOTATION)
- backend/src/main/resources/application.yml
  (app.upload.reference-images externalized)
- backend/src/test/java/com/handmadeart/ecommerce/HandmadeArtEcommerceApplicationTests.java
  (CustomOrderRequestRepository, CustomOrderImageRepository, QuotationRepository mocks added)
- backend/src/test/java/com/handmadeart/ecommerce/SecurityAuthorizationTest.java
  (CustomArtworkRequestService, QuotationService mocks added)
- project-docs/DEVELOPMENT_STATUS.md

Schema changes: None. V1–V5 unchanged. V5 already present; not yet verified against live DB.

DEC-003 (file upload limits): OPEN — image/* content-type only; no size limit.
DEC-004 (requotation): DEFERRED — one quotation per request only.
DEC-005 (advance payment rule): APPROVED and IMPLEMENTED.
DEC-008 (shipping/tracking): APPROVED and IMPLEMENTED.

PostgreSQL regression: Developer should run full db-integration suite (V5 not yet verified):
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration

Phase 3E.2 files created:
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/ShipmentResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/ShipmentCreateRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/customartwork/ShipmentStatusUpdateRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/service/CustomAdvancePaymentService.java
- backend/src/main/java/com/handmadeart/ecommerce/service/AdminProductionService.java
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkPhase2ServiceTest.java (27 tests)
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkPhase2ControllerTest.java (18 tests)

Phase 3E.2 files modified:
- backend/src/main/java/com/handmadeart/ecommerce/service/QuotationService.java
  (approveQuotation, rejectQuotation, resolveOwnedQuotation methods added)
- backend/src/main/java/com/handmadeart/ecommerce/controller/CustomArtworkController.java
  (approve/reject quotation, advance payment, shipment endpoints; @RequestMapping removed — explicit paths)
- backend/src/main/java/com/handmadeart/ecommerce/controller/AdminCustomArtworkController.java
  (production status, shipment create/update/get endpoints; AdminProductionService injected)
- backend/src/main/java/com/handmadeart/ecommerce/config/SecurityConfig.java
  (/api/v1/quotations/** → CUSTOMER added)
- backend/src/test/java/com/handmadeart/ecommerce/HandmadeArtEcommerceApplicationTests.java
  (ShipmentRepository mock added)
- backend/src/test/java/com/handmadeart/ecommerce/SecurityAuthorizationTest.java
  (CustomAdvancePaymentService, AdminProductionService mocks added)
- backend/src/test/java/com/handmadeart/ecommerce/CustomArtworkControllerTest.java
  (CustomAdvancePaymentService, AdminProductionService mocks added)
- project-docs/DEVELOPMENT_STATUS.md

## Prior Last Completed Task (Phase 3D.1)

Phase 3D.1 — Standard Checkout & Order Creation — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 161, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Endpoints: POST /api/v1/orders (create order from cart, pessimistic locking, inventory decrement, cart clear, address snapshot).

## Prior Last Completed Task (Phase 3C.2)

Phase 3C.2 — Cart Validation & Ownership — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 140, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Defect fixed:
- CartService.requirePurchasableProduct() previously allowed CUSTOM_AVAILABLE products into the cart.
  SRS FR-CART-01 and REST API Spec §18 state "only eligible ready-made products can be added to cart."
  CUSTOM_AVAILABLE follows the commissioned custom-artwork workflow, not the cart/checkout flow.
  Fix: productType check is now `== READY_MADE` (not just "!= PORTFOLIO_ONLY").
  CUSTOM_AVAILABLE products now return 409 PRODUCT_NOT_PURCHASABLE.

Ownership hardening:
- CartItemRepository.findByCartIdAndId(cartId, itemId) added — cart-scoped item lookup.
- CartService.requireOwnedItem() refactored to use findByCartIdAndId: ownership is enforced at the
  query level, so a foreign item ID cannot be resolved even before the application-level check runs.

Tests added (4 new):
- CART-S-22: addItem with CUSTOM_AVAILABLE product → ProductNotPurchasableException (FR-CART-01)
- CART-S-23: total is recalculated from current product prices (two READY_MADE items, distinct subtotals)
- CART-S-24: getCart with no existing cart → cartId is null, total is zero (no cart record created)
- CART-C-19: POST /cart/items with CUSTOM_AVAILABLE product → 409 PRODUCT_NOT_PURCHASABLE

Tests corrected:
- CART-S-21: product p2 type changed from CUSTOM_AVAILABLE to READY_MADE (previously used an
  impossible cart state for total-calculation test; no behaviour change — test still valid).
- CART-S-12..18: mocks updated from findById(itemId) to findByCartIdAndId(cartId, itemId) to
  align with the refactored ownership query.
- CART-S-08: assertion updated to expect "ready-made" in the exception message.

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/service/CartService.java
  (requirePurchasableProduct: CUSTOM_AVAILABLE now rejected; requireOwnedItem: cart-scoped query;
   checkStockAvailability: defensive guard updated; Javadoc updated)
- backend/src/main/java/com/handmadeart/ecommerce/repository/CartItemRepository.java
  (findByCartIdAndId added)
- backend/src/test/java/com/handmadeart/ecommerce/CartServiceTest.java
  (4 new tests added: CART-S-22/23/24; CART-S-21 corrected; mock stubs updated)
- backend/src/test/java/com/handmadeart/ecommerce/CartControllerTest.java
  (CART-C-19 added)
- project-docs/DEVELOPMENT_STATUS.md

Schema changes: None. V1–V5 unchanged.

DEC-009 (inventory concurrency strategy): OPEN — advisory stock check at cart time only; no reservation, no locking.

PostgreSQL regression: Developer should run full db-integration suite:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration

## Prior Last Completed Task (Phase 3C.1)

Phase 3C.1 — Cart Core APIs — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 136, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Cart endpoints implemented:
- GET    /api/v1/cart                — retrieve current user's cart (lazy empty if none)
- POST   /api/v1/cart/items         — add product to cart (lazy cart create; accumulate if duplicate)
- PUT    /api/v1/cart/items/{itemId}— update cart item quantity (stock check; ownership enforced)
- DELETE /api/v1/cart/items/{itemId}— remove single cart item (200 + updated cart)
- DELETE /api/v1/cart/items         — clear all cart items (204, cart record preserved)

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/exception/ProductNotPurchasableException.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/InsufficientStockException.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/cart/AddCartItemRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/cart/UpdateCartItemRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/cart/CartItemResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/cart/CartResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/service/CartService.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/CartController.java
- backend/src/test/java/com/handmadeart/ecommerce/CartControllerTest.java (18 tests: CART-C-01..CART-C-18)
- backend/src/test/java/com/handmadeart/ecommerce/CartServiceTest.java (21 tests: CART-S-01..CART-S-21)

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/exception/GlobalExceptionHandler.java (ProductNotPurchasableException + InsufficientStockException handlers)
- backend/src/test/java/com/handmadeart/ecommerce/HandmadeArtEcommerceApplicationTests.java (CartRepository + CartItemRepository mocks)
- backend/src/test/java/com/handmadeart/ecommerce/SecurityAuthorizationTest.java (CartService mock added)
- project-docs/DEVELOPMENT_STATUS.md

Schema changes: None. V1–V5 unchanged.

DEC-009 (inventory concurrency strategy): OPEN — advisory stock check at cart time only; no reservation, no locking.

PostgreSQL regression: Developer should run full db-integration suite:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration

## Prior Last Completed Task (Phase 3B.3)

Phase 3B.3 — Catalogue Integration Validation — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 97, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Defects fixed:
- Broad IllegalStateException → 409 in GlobalExceptionHandler replaced with narrow InventoryTypeConflictException handler. New exception class created: InventoryTypeConflictException.java. AdminCatalogueService.updateInventory() now throws InventoryTypeConflictException instead of IllegalStateException.
- Double countByProductId() call in AdminCatalogueService.addProductImage() consolidated to a single call.

Tests added (3 new service tests in AdminCatalogueServiceTest):
- PROD-08: updateProduct READY_MADE → PORTFOLIO_ONLY removes inventory row
- PROD-09: updateProduct PORTFOLIO_ONLY → READY_MADE creates inventory row
- IMG-01: removeProductImage with imageId belonging to a different product → ResourceNotFoundException

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/exception/InventoryTypeConflictException.java

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/exception/GlobalExceptionHandler.java (InventoryTypeConflictException handler; removed broad IllegalStateException handler)
- backend/src/main/java/com/handmadeart/ecommerce/service/AdminCatalogueService.java (InventoryTypeConflictException thrown; single countByProductId call)
- backend/src/test/java/com/handmadeart/ecommerce/AdminCatalogueServiceTest.java (3 tests added: PROD-08, PROD-09, IMG-01; INV-02 updated to assert InventoryTypeConflictException)
- backend/src/test/java/com/handmadeart/ecommerce/AdminCatalogueControllerTest.java (ACAT-16 mock updated to throw InventoryTypeConflictException)
- project-docs/DEVELOPMENT_STATUS.md

DEC-002 (JWT logout/revocation): OPEN — not implemented.
DEC-003 (file upload type/size limits): OPEN — image/* content-type validated; size limit not enforced.
DEC-009 (inventory concurrency strategy): OPEN — basic admin stock management only.

PostgreSQL regression: Developer must run full db-integration suite as the final Phase 3B regression checkpoint:
  mvn clean test -P db-integration-tests -Dspring.profiles.active=db-integration

## Prior Last Completed Task (Phase 3B.2)

Phase 3B.2 — Admin Catalogue Management APIs — COMPLETED and VERIFIED.

Build verification: `mvn clean test`
Result: Tests run: 94, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS.

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/CategoryRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/CategoryStatusRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/ProductRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/ProductStatusRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/RelatedProductsRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/InventoryUpdateRequest.java
- backend/src/main/java/com/handmadeart/ecommerce/dto/admin/InventoryResponse.java
- backend/src/main/java/com/handmadeart/ecommerce/service/AdminCatalogueService.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/AdminCategoryController.java
- backend/src/main/java/com/handmadeart/ecommerce/controller/AdminProductController.java
- backend/src/main/java/com/handmadeart/ecommerce/exception/DuplicateCategoryNameException.java
- backend/src/test/java/com/handmadeart/ecommerce/AdminCatalogueControllerTest.java (19 tests)
- backend/src/test/java/com/handmadeart/ecommerce/AdminCatalogueServiceTest.java (21 tests)

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/exception/GlobalExceptionHandler.java (DuplicateCategoryNameException + IllegalStateException handlers)
- backend/src/test/java/com/handmadeart/ecommerce/SecurityAuthorizationTest.java (AdminCatalogueService mock added)
- backend/src/test/java/com/handmadeart/ecommerce/HandmadeArtEcommerceApplicationTests.java (comment update)
- project-docs/DEVELOPMENT_STATUS.md

DEC-002 (JWT logout/revocation): OPEN — not implemented.
DEC-003 (file upload type/size limits): OPEN — image/* content-type validated; size limit not enforced.
DEC-009 (inventory concurrency strategy): OPEN — basic admin stock management only.

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

None. Phase 3E.2 complete and verified.

## Next Recommended Task

Phase 3F — Remaining Admin/Customer API Gap Review and Backend Final Validation.

Review approved REST API spec for any unimplemented endpoints across all modules (admin customer management,
customer profile, address management, order admin endpoints). Implement or document any approved gaps.
Run full db-integration test suite against live PostgreSQL to validate V5 migration.
Prepare for Phase 4 (Frontend React initialization).
