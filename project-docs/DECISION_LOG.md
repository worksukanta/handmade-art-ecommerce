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

Possible implementation approaches must be evaluated when Authentication development begins.

Affected Areas:

* Authentication
* Security
* Logout API
* Authentication Tests

Decision Required Before:

Final implementation of logout/token lifecycle behavior.

---

## DEC-003 — File Upload Type and Size Limits

Status: OPEN

Issue:

Reference-image validation is required, but exact supported file types and maximum file size must be finalized before implementation of upload validation.

Affected Areas:

* Custom Artwork Reference Images
* Product Images if applicable
* File Upload Validation
* File Upload Tests

Decision Required Before:

File upload implementation.

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

Status: OPEN

Issue:

Order cancellation exists where eligible, but exact allowed statuses/time window are not finalized.

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

Status: OPEN

Issue:

The React project has not yet been initialized, so Jest versus Vitest has not been selected.

Decision Rule:

Use the testing setup that best matches the actual React project configuration. Do not configure two test runners unnecessarily.

Decision Required Before:

Frontend automated testing.

---

## DEC-012 — E2E Framework

Status: OPEN

Issue:

Playwright and Cypress are both acceptable according to the approved test strategy.

Decision Rule:

Select one only.

Decision Required Before:

End-to-End test implementation.

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

