# DECISION LOG

## Project

Handmade & Custom Artwork E-Commerce Platform

## Purpose

This file records implementation decisions that were intentionally left unresolved by the approved project specifications or discovered during development.

Approved requirements and architecture must not be overridden through this file.

A decision should be recorded only when implementation requires an explicit choice.

---

## Decision Status Values

OPEN — decision is required.

APPROVED — implementation decision has been accepted.

DEFERRED — decision is intentionally postponed because it does not currently block development.

REJECTED — proposed approach was considered but rejected.

---

## DEC-001 — Payment Provider

Status: DEFERRED

Issue:

The approved design supports payment processing but does not require a specific external payment provider for the MVP.

Current Direction:

Use a provider-agnostic payment abstraction and sandbox/mock behavior until a provider decision becomes necessary.

Do not store raw card number, CVV, PIN, or other sensitive payment credentials.

Affected Areas:

* Payment
* Checkout
* Custom Artwork Advance Payment
* Payment Tests

Decision Required Before:

Provider-specific integration.

---

## DEC-002 — JWT Logout / Revocation Strategy

Status: OPEN

Issue:

JWT-based authentication is approved, but exact logout, token refresh, and token revocation semantics are not fully finalized.

Phase 5C MVP status: client-side token discard remains accepted for the stateless JWT demo. Server logout, refresh and revocation remain unimplemented. This decision stays OPEN for future token-lifecycle implementation; it does not block the current MVP.

Affected Areas:

* Authentication
* Security
* Logout API
* Authentication Tests

Decision Required Before:

Final implementation of logout/token lifecycle behavior.

---

## DEC-003 — File Upload Type and Size Limits

Status: DEFERRED

Issue:

The final business allowlist and size policy remain unresolved. The implemented MVP rejects empty files and non-image MIME declarations, generates storage filenames, and applies configurable 10MB file / 12MB request multipart limits with normalized 413 responses. Image content reads validate stored MIME metadata, enforce path containment, and now send a restrictive sandbox CSP. MIME declarations are not binary-content verification; no decoding, sanitization or malware-scanning pipeline is claimed.

Affected Areas:

* Custom Artwork Reference Images
* Product Images if applicable
* File Upload Validation
* File Upload Tests

Decision Required Before:

A stricter business upload policy or deployment beyond the accepted controlled MVP/demo.

---

## DEC-004 — Quotation Revision / Re-Quotation

Status: DEFERRED

Issue:

The approved MVP design currently treats quotation handling as a single quotation associated with a custom request.

There is no approved quotation revision/re-quotation workflow.

Current Direction:

Do not implement quotation revision unless the approved scope is changed.

Affected Areas:

* Custom Artwork
* Quotation
* UI
* Testing

---

### DEC-005 — Custom Artwork Advance Payment Rule

**Status:** APPROVED

For custom artwork orders, the quotation stores the authoritative
`advancePaymentAmount`.

Rules:
- Admin defines the advance amount when creating the quotation.
- advancePaymentAmount must be greater than 0.
- advancePaymentAmount must not exceed quotedAmount.
- Customer cannot supply or modify the authoritative advance amount.
- Advance payment is allowed only after the quotation has been approved.
- Payment must use the stored quotation advancePaymentAmount.
- No percentage-based advance calculation is inferred.
- Remaining-balance collection is outside the current MVP unless explicitly
  required by an approved specification.

---

## DEC-006 — Order Cancellation Eligibility

Status: DEFERRED

Issue:

Cancellation eligibility and timing remain unresolved. No customer cancellation endpoint/UI is implemented, and ADMIN cancellation remains rejected. Deferring this behavior is accepted for the current MVP/demo; implementation still requires an approved rule.

Affected Areas:

* Orders
* Customer UI
* Admin
* Testing

Decision Required Before:

Cancellation behavior implementation.

---

## DEC-007 — Tax and Delivery Charge

Status: DEFERRED

Issue:

No confirmed tax or delivery-charge calculation model is currently approved.

Current Direction:

Do not invent tax or delivery-charge formulas.

Use only the pricing/totals behavior explicitly supported by the approved implementation baseline.

Affected Areas:

* Cart
* Checkout
* Orders
* Payment

---

## DEC-008 — Shipping Integration

Status: APPROVED

Decision:

MVP shipping is status/tracking based.

External automated carrier/logistics API integration is not required.

Affected Areas:

* Orders
* Custom Artwork
* Shipment
* Admin

---

## DEC-009 — Inventory Concurrency Strategy

Status: APPROVED

Decision:

Checkout-time pessimistic locking.

* Cart stock checks remain advisory only (no reservation, no lock at cart stage).
* During standard checkout/order creation, load the relevant inventory rows using a database/JPA pessimistic write lock (SELECT … FOR UPDATE) within the checkout transaction.
* Revalidate all requested quantities while locks are held.
* If stock is insufficient, abort the transaction — no order is created and no inventory is decremented.
* If sufficient, create the order, order items, and decrement inventory atomically within the same transaction.
* Do not reserve stock when adding items to cart.
* Do not introduce optimistic-lock/version columns or a reservation subsystem.

Rationale:

Prevents overselling during concurrent checkout while fitting the existing schema and MVP scope. No schema changes are required.

Affected Areas:

* Inventory
* Checkout
* Order Creation
* Database Transactions
* Concurrency Tests

---

## DEC-010 — Default Address Behavior

Status: DEFERRED

Issue:

Address data supports a default indicator, but exact automatic default-selection behavior is not essential to initial development.

Current Direction:

Checkout must support an explicitly selected owned address.

Default-address convenience behavior may be finalized later if needed.

Affected Areas:

* Address
* Checkout
* Frontend

---

## DEC-011 — Frontend Test Runner

Status: APPROVED

Decision:

Vitest is the sole frontend unit/component test runner. React Testing Library, `@testing-library/jest-dom`, `@testing-library/user-event`, and jsdom provide behavior-oriented component testing and browser-like DOM support.

Rationale:

Vitest integrates directly with the existing React + TypeScript + Vite configuration, reuses Vite transformation, supports ESM without parallel Jest configuration, and provides the smallest maintainable setup for this application. Service modules are mocked at the centralized API boundary; tests do not call the live backend. MSW, Jest, Cypress, and Playwright are not introduced.

Scope:

Frontend unit and component behavior, route/auth/role behavior, form and workflow state gates, centralized API-error normalization, and focused regressions. Dedicated browser E2E testing remains separate and DEC-012 is DEFERRED.

Implementation:

- Global jsdom setup: `frontend/src/test/setup.ts`.
- Shared render/auth helpers: `frontend/src/test/helpers.tsx`.
- Local/watch command: `npm test`.
- CI/non-watch command: `npm run test:run`.

---

## DEC-012 — E2E Framework

Status: DEFERRED

Decision:

No browser E2E framework is added for the MVP. Browser automation is deferred as a future enhancement.

Rationale:

The backend automated suite, the 65-test Vitest/React Testing Library baseline, and the previously completed live PostgreSQL/API workflow verification provide proportionate MVP regression confidence without adding another framework and browser-binary maintenance surface. Playwright or Cypress may be selected later if repeatable cross-browser UI journeys become a delivery requirement; only one should then be adopted.

Scope:

Automated browser E2E implementation beyond the MVP. Phase 5B used real frontend/backend availability checks and live, unmocked API workflows. Those earlier agent runs did not claim visual/browser checks because no controllable browser surface was available. The Phase 5C request explicitly records responsive/manual UI review as accepted by the user. This is user-provided acceptance, not a new automated browser run.

---

## DEC-013 — Database Migration Framework

Status: APPROVED

Decision:

Flyway is used as the sole database migration and schema management framework.

Rationale:

The approved Database Design & ERD is the schema source of truth, not Hibernate. Flyway ensures schema changes are explicit, versioned, reproducible, and auditable — consistent with the project's requirement for controlled PostgreSQL schema evolution. Hibernate `ddl-auto` is set to `none` permanently. Liquibase was not selected; one migration framework only is used.

Implementation:

- Flyway added: `flyway-core` + `flyway-database-postgresql` (required for Flyway 10 / Spring Boot 3.3+).
- Migration location: `classpath:db/migration`.
- Naming convention: `V<version>__<lowercase_description>.sql`.
- Flyway is disabled in the default test profile so unit/context tests do not require a live database.
- Flyway is enabled in the `db-integration` test profile for integration testing.
- V1__migration_baseline.sql established as the first migration (no domain tables; Phase 2A only).

Affected Areas:

* All backend phases from Phase 2B onward
* All database integration tests
* CI/CD pipeline (when established)

