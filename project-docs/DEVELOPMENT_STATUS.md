# DEVELOPMENT STATUS

## Project

Handmade & Custom Artwork E-Commerce Platform

IBM Technical Training Capstone Project

## Current Phase

Phase 2 — Database Foundation

## Current Module

Phase 2A — PostgreSQL Infrastructure + Migration Baseline — Completed

## Overall Status

Spring Boot backend foundation initialized and build-verified (Phase 1).

Database infrastructure established: Flyway added, DataSource re-enabled, migration baseline in place (Phase 2A).

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
* [ ] Phase 2B — Identity Entities
* [ ] Phase 2C — Catalogue + Inventory Entities
* [ ] Phase 2D — Commerce Entities
* [ ] Phase 2E — Custom Artwork Entities
* [ ] Phase 2F — Database Integration Validation

## Backend Status

Spring Boot 3.5.0 project initialized and build-verified.

Java target: 21 (running on Java 26.0.1 — compatible).

Maven configured (pom.xml with Spring Web, Spring Data JPA, Spring Security, Validation, PostgreSQL driver, Flyway Core, Flyway PostgreSQL provider, Spring Boot Test).

Base package structure prepared: com.handmadeart.ecommerce with config, controller, dto/request, dto/response, entity, exception, repository, security, service, util.

DataSource auto-configuration: RE-ENABLED for runtime. DataSource and Flyway excluded in default test profile only (no PostgreSQL required for context-load and unit tests).

PostgreSQL database name: handmade_art_ecommerce. Connection parameters via environment variables: DB_URL, DB_USERNAME, DB_PASSWORD.

Flyway: Added (flyway-core + flyway-database-postgresql). Enabled in runtime and db-integration test profile. Disabled in default test profile. Migration location: classpath:db/migration.

Hibernate schema policy: ddl-auto: none (permanent). Flyway is sole schema authority. Hibernate will not create/mutate schema.

Migration baseline: V1__migration_baseline.sql created. No domain tables yet (domain schema begins Phase 2B).

Migration naming convention: V<version>__<lowercase_description>.sql

Temporary dev security configuration in place (Phase 1 only — to be replaced in Phase 3).

Build verification: mvn clean test — PASSED. 1 test, 0 failures, 0 errors (default profile).

## Frontend Status

Not started.

## Database Status

PostgreSQL: DataSource configured, driver present, Flyway dependency added.

Runtime DataSource auto-configuration: ENABLED.

Flyway: Configured. Will execute V1 baseline migration on first application startup against a real PostgreSQL instance.

Hibernate ddl-auto: none (Flyway owns schema).

PostgreSQL connectivity: NOT VERIFIED at this stage — no configured PostgreSQL test database available in the build environment during Phase 2A execution. Connectivity verification deferred to developer local setup or Phase 2B where the first domain migration will confirm end-to-end connectivity.

Flyway runtime migration: NOT VERIFIED for same reason (requires live PostgreSQL).

Database integration test class: DatabaseInfrastructureIntegrationTest created. Tagged @Tag("db-integration"). Excluded from default mvn clean test. Run with: mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration (requires DB_URL, DB_USERNAME, DB_PASSWORD pointing at handmade_art_ecommerce_test).

## API Status

REST API contract approved.

Implementation not started.

## Testing Status

Test strategy approved.

Tests implemented: 2
- HandmadeArtEcommerceApplicationTests.contextLoads (default profile — no DB required)
- DatabaseInfrastructureIntegrationTest (db-integration profile — requires live PostgreSQL)

Tests executed (default profile): 1

Tests passed (default profile): 1

Tests failed: 0

Database integration tests: NOT EXECUTED — require live PostgreSQL. See README for how to run.

## Current Known Issues

PostgreSQL connectivity and Flyway runtime migration execution NOT VERIFIED in this session — no configured PostgreSQL test database was available. Developer must configure DB_URL, DB_USERNAME, DB_PASSWORD and run the db-integration profile to verify end-to-end.

Note: Mockito dynamic-agent JVM warnings on Java 26 suppressed via -XX:+EnableDynamicAgentLoading in Surefire config.

## Pending Decisions

See DECISION_LOG.md.

DEC-013 (Flyway as migration framework): APPROVED — recorded in DECISION_LOG.md.

DEC-002 (JWT logout/revocation), DEC-003 (file upload limits), DEC-005 (advance payment rule), DEC-006 (order cancellation eligibility), DEC-009 (inventory concurrency), DEC-011 (frontend test runner), DEC-012 (E2E framework) remain OPEN but do not block current phase.

## Last Completed Task

Phase 2A — PostgreSQL Infrastructure + Migration Baseline.

Files created:
- backend/src/main/resources/db/migration/V1__migration_baseline.sql
- backend/src/test/resources/application-db-integration.yml
- backend/src/test/java/com/handmadeart/ecommerce/DatabaseInfrastructureIntegrationTest.java

Files modified:
- backend/pom.xml (added Flyway dependencies, db-integration tag exclusion)
- backend/src/main/resources/application.yml (re-enabled DataSource, added Flyway config, updated DB name)
- backend/src/test/resources/application.yml (added FlywayAutoConfiguration to exclusions, added profile docs)
- backend/README.md (PostgreSQL setup, Flyway usage, db-integration test instructions)
- project-docs/DECISION_LOG.md (DEC-013 added)

## Current Task

None. Awaiting Phase 2B prompt.

## Next Recommended Task

Phase 2B — Identity and Customer Database Model.

Define JPA entities for AppUser and Address tables per the approved Database Design & ERD (Sections 3.1 and 3.2). Create Flyway migration V2. Write repository interfaces and integration tests. Re-verify end-to-end PostgreSQL connectivity as part of this phase.
