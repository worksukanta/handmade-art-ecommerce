# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 2 — Database Foundation

## Current Module

Phase 2B — Identity and Customer Database Model — Completed

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway added, DataSource re-enabled, migration baseline in place (Phase 2A).

Identity and Address persistence model implemented: AppUser, Address entities, repositories, V2 migration, and db-integration tests added (Phase 2B).

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
* [-] Database Foundation
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
* [ ] Phase 2C — Catalogue + Inventory Entities
* [ ] Phase 2D — Commerce Entities
* [ ] Phase 2E — Custom Artwork Entities
* [ ] Phase 2F — Database Integration Validation

## Backend Status

Spring Boot 3.5.0 project initialized and build-verified.

Java target: 21 (running on Java 26.0.1 — compatible).

Maven configured (pom.xml with Spring Web, Spring Data JPA, Spring Security, Validation, PostgreSQL driver, Flyway Core, Flyway PostgreSQL provider, Spring Boot Test).

Base package structure prepared.

DataSource auto-configuration: RE-ENABLED for runtime.

PostgreSQL database name: handmade_art_ecommerce. Connection via DB_URL, DB_USERNAME, DB_PASSWORD.

Flyway: Enabled. Migration location: classpath:db/migration.

Hibernate schema policy: ddl-auto: none (permanent). Flyway is sole schema authority.

Migrations:
- V1__migration_baseline.sql — baseline marker (no domain tables)
- V2__create_identity_tables.sql — app_user and address tables with all approved constraints, indexes, and FK

Entities created:
- UserRole (enum: CUSTOMER, ADMIN)
- AppUser (entity: app_user table)
- Address (entity: address table)

Repositories created:
- AppUserRepository (findByEmailIgnoreCase, existsByEmailIgnoreCase, countByRole, findByEmailLowerCase)
- AddressRepository (findByUserId, findByUserIdAndId, findByUserIdAndIsDefaultTrue, countByUserId)

Temporary dev security configuration in place (Phase 1 only — to be replaced in Phase 3).

Build verification: mvn clean test — PASSED. 1 test, 0 failures, 0 errors (default profile).

## Frontend Status

Not started.

## Database Status

PostgreSQL: DataSource configured, driver present, Flyway dependency added.

Runtime DataSource auto-configuration: ENABLED.

Flyway: Configured. V1 and V2 migration scripts present.

Hibernate ddl-auto: none (Flyway owns schema).

PostgreSQL connectivity: NOT VERIFIED — PostgreSQL 18 service is running on this machine but the postgres superuser password is not known/available in the current environment. pg_hba.conf requires scram-sha-256 for all connections. Developer must supply valid credentials via DB_URL, DB_USERNAME, DB_PASSWORD to verify end-to-end connectivity and run db-integration tests.

Flyway V1 applied: NOT VERIFIED (requires live DB).
Flyway V2 applied: NOT VERIFIED (requires live DB).
JPA persistence: NOT VERIFIED against PostgreSQL (requires live DB).

Database integration tests: Created. Tagged @Tag("db-integration"). Excluded from default mvn clean test. Run with:
  mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration

## API Status

REST API contract approved.

Implementation not started.

## Testing Status

Test strategy approved.

Tests implemented: 3 classes
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile)
- DatabaseInfrastructureIntegrationTest (db-integration — Phase 2A infra verification)
- IdentityPersistenceIntegrationTest (db-integration — Phase 2B: 11 tests covering AppUser and Address persistence, constraints, and ownership queries)

Tests executed (default profile): 1 (HandmadeArtEcommerceApplicationTests.contextLoads)

Tests passed (default profile): 1

Tests failed: 0

Database integration tests: NOT EXECUTED — require live PostgreSQL with known credentials.

## Current Known Issues

PostgreSQL superuser password not available in this session — db-integration tests cannot be executed until a developer configures valid DB_URL, DB_USERNAME, DB_PASSWORD pointing at the test database.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED.
DEC-010 (default address behavior): DEFERRED — is_default field persisted; behavior logic deferred to Phase 3+.
DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-005 (advance payment rule), DEC-006 (order cancellation eligibility), DEC-009 (inventory concurrency), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN but do not block current phase.

## Last Completed Task

Phase 2B — Identity and Customer Database Model.

Files created:
- backend/src/main/java/com/handmadeart/ecommerce/entity/UserRole.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/AppUser.java
- backend/src/main/java/com/handmadeart/ecommerce/entity/Address.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/AppUserRepository.java
- backend/src/main/java/com/handmadeart/ecommerce/repository/AddressRepository.java
- backend/src/main/resources/db/migration/V2__create_identity_tables.sql
- backend/src/test/java/com/handmadeart/ecommerce/IdentityPersistenceIntegrationTest.java

Files modified:
- backend/src/main/java/com/handmadeart/ecommerce/entity/AppUser.java (constructor visibility fix)
- backend/src/main/java/com/handmadeart/ecommerce/entity/Address.java (constructor visibility fix)
- project-docs/DEVELOPMENT_STATUS.md

## Current Task

None. Awaiting Phase 2C prompt.

## Next Recommended Task

Phase 2C — Catalogue and Inventory Database Model.

Define JPA entities for Category, Product, ProductImage, ProductRelated, and Inventory per the approved Database Design & ERD (Sections 3.3–3.6, 3.12). Create Flyway migration V3.
