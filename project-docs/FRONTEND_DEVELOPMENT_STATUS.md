# Frontend Development Status

## Current state

- Frontend status: **PHASE 4A.1 COMPLETE / FOUNDATION INITIALIZED**
- Backend status: **COMPLETE / API baseline frozen** (except endpoints explicitly blocked by open/deferred decisions)
- Frontend branch: `phase-4a-frontend-foundation`
- Current milestone: Phase 4A.1 — React foundation initialization
- `frontend/` state: Vite React + TypeScript application initialized with a minimal routed shell and centralized Axios client

## Approved frontend stack and setup facts

- Application: React single-page application
- Build tool: Vite
- Language: TypeScript
- Package manager: npm
- Routing: React Router in declarative SPA mode
- API access: Axios through a shared client using `VITE_API_BASE_URL` and the `/api/v1` contract
- State: React built-in state and Context are sufficient; no external state-management library is approved or required
- Component testing: React Testing Library; test runner remains open under DEC-011
- CSS/UI library and visual theme: **UNDECIDED**

## Phase 4A.1 verification

- Foundation files: `src/main.tsx`, `src/app/router.tsx`, `src/components/layout/AppLayout.tsx`, `src/pages/HomePage.tsx`, `src/pages/NotFoundPage.tsx`, `src/services/apiClient.ts`, and `src/styles/global.css`
- Environment: `frontend/.env.example` provides `VITE_API_BASE_URL=http://localhost:8080/api/v1`; real `.env` files remain ignored
- Dependency installation: succeeded with 0 reported vulnerabilities
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS (scaffolded Oxlint)
- Tests: not configured; DEC-011 remains open

## Minimum frontend implementation map

### 1. Foundation / app shell

- Pages/features: React SPA shell, public/customer/admin layouts, role-aware navigation, routing and guards, global loading/error/empty/notification patterns
- API groups: shared `/api/v1` configuration; authentication session restoration via `GET /auth/me`
- Auth: mixed — public, CUSTOMER, and ADMIN routes
- Shared needs: router, API client/service base, AuthContext, protected/role routes, layouts, header/footer, notification and async-state components

### 2. Authentication

- Pages/features: registration, login, client-side logout/session clearing, session restoration
- API groups: `POST /auth/register`, `POST /auth/login`, `GET /auth/me`; do not call unresolved `POST /auth/logout`
- Auth: public entry pages; authenticated session state after login
- Shared needs: auth service/context, credential forms, validation/error mapping, role-aware redirect

### 3. Public catalogue and product

- Pages/features: catalogue/home, category navigation, search/filter/sort/pagination, product details, images, availability, related products
- API groups: `GET /categories`, `GET /categories/{id}`, `GET /products`, `GET /products/{id}`, `GET /products/{id}/related-products`
- Auth: public
- Shared needs: product/category services, product cards/gallery, filters, pagination, price/availability display, loading/empty/error states

### 4. Cart

- Pages/features: add item, cart view, quantity update, item removal, clear cart, cart badge
- API groups: `GET /cart`, `POST /cart/items`, `PUT /cart/items/{itemId}`, `DELETE /cart/items/{itemId}`, `DELETE /cart/items`
- Auth: CUSTOMER
- Shared needs: cart service, quantity controls, cart summary, server-conflict handling, lightweight shared cart count

### 5. Profile and addresses

- Pages/features: view/edit profile; list/create/edit/delete addresses; explicit checkout address selection
- API groups: `GET/PUT /account/profile`, `GET/POST /account/addresses`, `PUT/DELETE /account/addresses/{id}`
- Auth: CUSTOMER, owned data only
- Shared needs: account service, profile/address forms, validation messages, address cards/selector

### 6. Checkout, payment, and orders

- Pages/features: checkout validation, address selection, order creation, provider-agnostic payment initiation/status, order history/detail, payment history, shipment view
- API groups: `POST /checkout/validate`, `POST /orders`, `GET /orders`, `GET /orders/{id}`, `POST/GET /orders/{id}/payments`, `GET /orders/{id}/shipment`; cancellation remains unavailable while DEC-006 is open
- Auth: CUSTOMER, owned data only
- Shared needs: checkout/order/payment services, totals and order summaries, payment/shipment status components, conflict handling

### 7. Custom artwork customer workflow

- Pages/features: submit request, upload reference images, request list/detail, quotation display and approve/reject, advance payment, production/shipment tracking
- API groups: `/custom-requests`, `/custom-requests/{id}`, `/custom-requests/{id}/images`, `/custom-requests/{id}/quotation`, `/quotations/{id}/approve|reject`, `/custom-requests/{id}/payments`, `/custom-requests/{id}/shipment`
- Auth: CUSTOMER, owned data only
- Shared needs: custom-request service, upload control, status/timeline display, quotation/payment/shipment components

### 8. Admin catalogue

- Pages/features: product list/create/edit/status, product images and related products, categories, inventory
- API groups: `/admin/products`, `/admin/products/{id}`, product status/images/related-products, `/admin/categories`, `/admin/inventory`
- Auth: ADMIN
- Shared needs: admin layout/guards, management tables/forms, upload/gallery controls, pagination/filter controls, inventory editor

### 9. Admin orders, custom artwork, and customers

- Pages/features: order list/detail/status; custom-request queue/detail/review/quotation/production/shipment; customer list/detail
- API groups: `/admin/orders`, `/admin/payments/{id}`, `/admin/custom-requests`, `/admin/quotations/{id}`, `/admin/shipments`, `/admin/customers`
- Auth: ADMIN
- Shared needs: admin tables/detail views, status filters/transitions, quotation form, shipment controls, pagination and shared workflow-status components

### 10. Final testing and integration

- Pages/features: contract integration, responsive/accessibility verification, auth/role/error-state coverage, critical customer/admin journey regression
- API groups: all implemented groups used above
- Auth: public, CUSTOMER, and ADMIN scenarios
- Shared needs: React Testing Library setup, one DEC-011 runner, one DEC-012 E2E framework, API mocking/fixtures, test environment configuration

## Open decisions and constraints

- DEC-011 frontend test runner: **OPEN** — do not choose Jest or Vitest yet.
- DEC-012 E2E framework: **OPEN** — do not choose Cypress or Playwright yet.
- CSS/UI library and final visual theme: **UNDECIDED**; this foundation uses minimal global CSS only.
- Payment-provider-specific UI remains deferred; implement only provider-agnostic initiation and status behavior until DEC-001 changes.
- Server-side logout remains unavailable while DEC-002 is open; frontend logout clears client authentication state only.
- Order cancellation remains unavailable while DEC-006 is open.

## Documentation/repository alignment

- The UI/UX specification flags the lack of an Admin product-list endpoint, but the later backend/API baseline implements `GET /api/v1/admin/products`; the UI document's warning is stale relative to the accepted implementation.
- The UI/UX specification mentions `PUT /admin/shipments/{id}`, while the accepted backend exposes shipment creation as `POST /api/v1/admin/shipments` and status updates as `PATCH /api/v1/admin/shipments/{id}/status`; frontend implementation must follow the frozen REST/backend contract.
- Some development-status history still says frontend initialization is next/not started; this file records Phase 4A.0 planning as complete while scaffolding remains not started.

## Next recommended task

Begin Phase 4A.2 — frontend architecture and an auth-ready shell. Keep DEC-011 and DEC-012 open unless separately approved.
