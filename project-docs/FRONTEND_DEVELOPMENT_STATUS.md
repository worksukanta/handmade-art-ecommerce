# Frontend Development Status

## Current state

- Frontend status: **PHASE 4G COMPLETED / ADMIN CUSTOM ARTWORK OPERATIONS INTEGRATED**
- Backend status: **COMPLETE / API baseline frozen** (except endpoints explicitly blocked by open/deferred decisions)
- Frontend branch: `phase-4f-custom-artwork`
- Current milestone: Phase 4G — Admin custom artwork operations
- `frontend/` state: Vite React + TypeScript application covering public browsing, standard customer commerce, and the end-to-end customer/Admin custom-artwork workflow through delivery

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

## Phase 4A.2 verification

- Auth contracts: exact register, login, login-user-summary, and current-user DTO types established for `POST /auth/register`, `POST /auth/login`, and `GET /auth/me`
- Session: JWT stored under `handmade-art.access-token` through one `localStorage` abstraction; `/auth/me` restores authoritative user state at startup; invalid/expired tokens are cleared
- API auth: the shared Axios request interceptor attaches the Bearer token; 401 responses clear local auth state without refresh or redirect-loop behavior
- Routing: reusable authenticated and CUSTOMER/ADMIN role guards protect approved placeholder routes; guards remain UX controls and backend authorization remains authoritative
- Shell: public, CUSTOMER, and ADMIN navigation states with local-only sign-out; no backend logout call
- Error handling: backend `ApiError` responses normalize to safe status/message/details for future UI use
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS with no warnings
- Decisions: DEC-002, DEC-011, and DEC-012 remain OPEN

## Phase 4B verification

- Login: accessible email/password form integrated with `POST /auth/login`, normalized failures, duplicate-submit prevention, and CUSTOMER/ADMIN post-login routing
- Registration: approved name/email/password/optional-phone contract integrated with `POST /auth/register`; confirmation password remains frontend-only; success redirects to login without auto-login
- Session recovery: a persisted token is verified through `GET /auth/me`; protected content remains hidden during initialization; invalid/expired sessions are cleared
- Sign-out: local token/context clearing only; no server logout or revocation is claimed while DEC-002 remains open
- Navigation: anonymous, CUSTOMER, and ADMIN states remain role-aware; route guards are UX controls and backend authorization remains authoritative
- Forms: reusable accessible field, error-summary, and loading-submit primitives added with backend-aligned validation constraints
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS with no warnings
- Decisions: DEC-002, DEC-011, and DEC-012 remain OPEN

## Phase 4C verification

- Routes: `/` provides catalogue search/filter/sort/pagination; `/products/:id` provides public product details
- API integration: `GET /products`, `GET /categories`, and `GET /products/{id}` through a dedicated typed catalogue service
- Presentation: reusable product cards, resilient product images, primary/order-aware gallery, availability/type messaging, and embedded related products
- Images: frontend renders the backend-provided `imageUrl`; it does not construct paths from `storage_reference`
- States: responsive loading skeletons, retryable errors, empty results, product-not-found handling, and broken-image fallback
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS with no warnings
- Runtime integration: public `GET /products` and `GET /categories` returned 200; the current database contained no public catalogue records
- Backend/API issues: none discovered
- Decisions: DEC-011 and DEC-012 remain OPEN

## Phase 4D verification

- Cart: authenticated cart route, server-authoritative item pricing/totals, quantity updates, removal, confirmed clear, customer navigation count, and product-detail Add to Cart for eligible READY_MADE products
- Profile/addresses: typed account service, editable name/phone profile, read-only email, and owned address list/create/edit/delete with explicit default flag support
- Checkout: explicit owned-address selection, non-mutating `POST /checkout/validate`, separate confirmed `POST /orders`, duplicate-submit prevention, cart reset, and minimal order-created result
- Product eligibility: CUSTOM_AVAILABLE points to the future custom-request flow; PORTFOLIO_ONLY never exposes standard cart behavior
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS with no warnings
- Backend/API defects: none discovered
- DEC-010 remains DEFERRED; checkout never silently chooses an address
- DEC-011 and DEC-012 remain OPEN

## Phase 4E verification

- Routes/navigation: protected CUSTOMER `/orders` and `/orders/:id` routes, plus an Orders entry in authenticated customer navigation
- Order history: paginated `GET /orders?page=&size=` integration with authoritative ID/date/recipient/location/total/status data, responsive cards, empty state, retryable error state, and detail navigation
- Order detail: `GET /orders/{id}` integration displaying purchase-time item name/price/quantity/line-total snapshots, address snapshot, authoritative subtotal/total, order status, and timestamps
- Payments: `GET /orders/{id}/payments` status/history and `POST /orders/{id}/payments` initiation using only `{paymentMethod: "SANDBOX"}`; amount remains backend-derived and payment action appears only for `PENDING_PAYMENT` orders
- Payment outcomes: actual `PENDING`, `SUCCESS`, or `FAILED` response status is displayed; duplicate submission is disabled and 409 `ORDER_NOT_PAYABLE` is presented as a contextual business conflict followed by an order/payment refresh
- Shipment: `GET /orders/{id}/shipment` displays backend carrier, tracking reference, status, estimated delivery date, shipped/delivered timestamps; a 404 is treated as the normal not-yet-created state without disabling order details
- Checkout success: `/checkout/success/:orderId` now links to the authoritative order detail/payment view, survives refresh without navigation state, and never equates order creation with payment success
- Status models: exact backend order (`PENDING_PAYMENT`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`), payment (`PENDING`, `SUCCESS`, `FAILED`), payment-purpose (`FULL`, `ADVANCE`, `REMAINING`), and shipment (`PENDING`, `SHIPPED`, `DELIVERED`) enums are typed and humanized without changing semantics
- Error/loading/accessibility: independent page/payment/shipment loading and error states, normalized API failures, semantic sections/lists/definition lists/time elements, textual status badges, labelled actions, focus-visible behavior, and responsive layouts
- Production build: `npm run build` — PASS
- Lint: `npm run lint` — PASS with no warnings
- Frontend automated tests: not added because DEC-011 remains open and no runner is configured
- Runtime integration: not performed during this phase; build-time contract integration was verified against frozen controllers, DTOs, enums, services, and controller tests
- Backend/API defects: none discovered
- Blocked: order cancellation remains excluded while DEC-006 is OPEN; external/provider callback payment integration remains excluded while DEC-001 is DEFERRED

## Phase 4F verification

- Routes/navigation: protected CUSTOMER routes `/custom-requests`, `/custom-requests/new`, and `/custom-requests/:id`, plus a customer navigation entry and catalogue entry point for `CUSTOM_AVAILABLE` products. Anonymous catalogue users retain the requested destination through the existing login-return flow; `READY_MADE` cart behavior and `PORTFOLIO_ONLY` non-purchasability remain unchanged.
- Request creation: exact `POST /custom-requests` DTO fields (`productType`, `description`, optional `designTheme`, `preferredColors`, `dimensionsSize`, `budgetRange`, `requiredDeliveryDate`, and `additionalInstructions`) with accessible validation, normalized errors, and duplicate-submit prevention. Catalogue products provide inspiration text only because the backend request contract has no product ID relationship.
- History/detail: paginated and status-filtered `GET /custom-requests`, owned `GET /custom-requests/{id}`, complete request fields, reference-image metadata, request status badge, and a state-oriented workflow timeline covering every accepted request status without invented timestamps or percentages.
- Reference images: `POST /custom-requests/{id}/images` using `multipart/form-data` field `file`; selected filename, independent upload state, success/error feedback, and refreshed backend metadata. The backend permits repeated uploads for an owned request and exposes no customer image-content URL, so the frontend does not construct filesystem URLs or render unsafe previews.
- Quotation: `GET /custom-requests/{id}/quotation`, `POST /quotations/{id}/approve`, and `POST /quotations/{id}/reject`. Exact quoted/advance amounts, expiry, estimated delivery, notes/terms, and status are shown. Decision buttons appear only for a non-expired `PENDING` quotation on a `QUOTED` request; approval and rejection use separate confirmed mutations and refresh authoritative state. No rejection body or re-quotation behavior is invented.
- Advance payment: `GET/POST /custom-requests/{id}/payments`; the only request field is `{paymentMethod: "SANDBOX"}`. Payment appears only for an `APPROVED` request without a successful ADVANCE payment, displays the stored quotation advance amount as read-only, and presents the returned payment status/history. No card data, percentage inference, or provider-specific UI is used.
- Shipment: `GET /custom-requests/{id}/shipment` is custom-request-specific. Carrier, tracking reference, status, estimated date, shipped time, and delivered time are displayed when supplied. A missing shipment is treated as normal and does not blank the request detail.
- Loading/errors/accessibility: independent page, quotation, image-upload, payment, and shipment states; retryable primary failures; section-scoped related-data errors; refreshed authoritative data after workflow conflicts; semantic headings, lists, definition lists, labels, status text, time elements, keyboard controls, and responsive layouts.
- Production build: `npm run build` — PASS.
- Lint: `npm run lint` — PASS with no warnings.
- Frontend automated tests: not added because DEC-011 remains open and no test runner is configured.
- Runtime integration: against the fixed backend on port 8081, CUSTOMER registration/login, request creation, history listing, owned detail retrieval, and a valid PNG reference upload were verified. Request `710` remained `REQUESTED`; upload returned 201, persisted image metadata, appeared in detail, and wrote the physical file under the configured root. Browser-driven visual verification was unavailable because no controllable browser was connected. The stale backend process already listening on port 8080 still ran pre-fix code and returned 500 for the same upload.
- ADMIN dependency: admin review must transition the request to `UNDER_REVIEW` and create the single MVP quotation before customer quotation decision, advance payment, production, and shipment states can be exercised end-to-end. Database rows were not edited manually.
- Backend/API blockers: none in the current fixed code. The running port-8080 process must be restarted before the frontend configured for that port can use the upload fix.
- Decisions: DEC-001 provider integration remains DEFERRED; DEC-003 upload size limits remain OPEN; DEC-004 re-quotation remains DEFERRED; DEC-006 order cancellation remains OPEN; DEC-011 frontend test runner and DEC-012 E2E framework remain OPEN.

## Phase 4G verification

- Routes/navigation: protected ADMIN routes `/admin/custom-requests` and `/admin/custom-requests/:id`, plus distinct `Custom requests` and preserved `/admin/products` navigation entries.
- Queue: `GET /admin/custom-requests?status=&page=&size=` with server-side status filtering and pagination; displays request/customer IDs, product type, description, status, submitted/updated timestamps, loading/empty/retry states, and detail navigation.
- Detail discovery: `GET /admin/custom-requests/{id}` plus request-scoped `/quotation`, `/payments`, and `/shipment` reads. All submitted requirements, review information, timestamps, and reference-image metadata are shown. No filesystem path or fabricated image URL is rendered because no ADMIN image-content endpoint exists.
- Review: exact `PATCH /admin/custom-requests/{id}/review` DTO `{decision: "ACCEPT"|"REJECT", notes?}`. REQUESTED exposes confirmed review start; UNDER_REVIEW exposes confirmed terminal rejection. Mutations prevent duplicate submission, refresh authoritative state, and contextualize stale-state 409 responses.
- Quotation: exact `POST /admin/custom-requests/{id}/quotation` fields `quotedAmount`, `advanceAmount`, `estimatedDeliveryDate?`, `expiryAt`, and `notesTerms?`. The form requires positive quoted/fixed-advance amounts, prevents advance exceeding quoted amount, requires a future expiry, performs no percentage inference, and explains that the advance is fixed. Existing quotations remain read-only under DEC-004.
- Customer decision/payment visibility: request-scoped quotation and payment reads expose authoritative quotation status, decision time, payment purpose/status/amount/method/timestamps. ADMIN receives no approve/reject controls on behalf of the customer.
- Production: successful sandbox advance payment moves the request to IN_PRODUCTION. The UI exposes only the accepted ADMIN request transition `IN_PRODUCTION -> COMPLETED`; shipping/delivery progression uses shipment status operations.
- Shipment: exact `POST /admin/shipments` DTO with `customOrderRequestId` and optional `carrierName`, `trackingReference`, `estimatedDeliveryDate`; request-scoped shipment retrieval; accepted `PATCH /admin/shipments/{id}/status` progression `PENDING -> SHIPPED -> DELIVERED`. No external carrier integration is implied.
- Runtime integration: on a fresh backend at the configured main port, request `714` was registered/submitted with one reference image, found in the ADMIN queue, reviewed to UNDER_REVIEW, and quoted at 15000 with a fixed 5000 advance. The customer saw and approved the PENDING quotation, completed a successful 5000 SANDBOX advance payment, and the request reached IN_PRODUCTION. ADMIN observed one payment, marked production COMPLETED, created a PENDING shipment, and progressed it through SHIPPED to DELIVERED. CUSTOMER reads then returned DELIVERED request/shipment state and the authoritative tracking reference.
- Browser verification: unavailable because no controllable in-app or extension browser was connected; runtime workflow verification used the same live HTTP contracts. No E2E framework was added while DEC-012 remains open.
- Production build: `npm run build` - PASS.
- Lint: `npm run lint` - PASS with no warnings.
- Backend/API blockers: none for Phase 4G.
- Remaining decisions: DEC-001, DEC-003, DEC-004, DEC-006, DEC-011, and DEC-012 retain their recorded status. Broader ADMIN catalogue/inventory management is not marked complete.

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
- Historical development-status entries may still describe frontend initialization as pending; this file is the current frontend-specific status through Phase 4B.

## Phase 4H — Admin catalogue, inventory, and standard-order operations

- Routes: added ADMIN-only `/admin/products`, `/admin/products/new`, `/admin/products/:id`, `/admin/products/:id/edit`, `/admin/categories`, `/admin/inventory`, `/admin/orders`, and `/admin/orders/:id` routes behind the existing role guard. Admin navigation now links Products, Custom Requests, and Orders; catalogue pages provide contextual Categories and Inventory links.
- Products: paginated all-status list, detail, create, edit, ACTIVE/INACTIVE updates, exact product-type selection, category assignment, price, and description editing. Writes use accepted ADMIN DTOs and refresh authoritative server state.
- Categories: all-status list plus create, update, and ACTIVE/INACTIVE operations. The backend has no hierarchy or delete endpoint, so neither is represented.
- Images: multipart upload and deletion from product detail. The UI displays response metadata and resolves only the backend-provided public `imageUrl`; it does not display storage references. The backend exposes no ordering or primary-image mutation.
- Related products: view, add, and remove through the accepted replacement endpoint. Already-related products are excluded and mutations prevent repeat submission.
- Inventory: paginated authoritative quantity display and non-negative whole-number replacement through `/admin/inventory/{productId}`. Product updates are not used to alter stock.
- Standard orders: paginated list and detail with customer, item snapshots, delivery-address snapshot, totals, payments, and shipment. Only accepted transitions are offered (`PENDING_PAYMENT -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED`); cancellation remains absent under DEC-006. Mutations confirm progression, disable repeat submission, refresh server state, and contextualize HTTP 409 conflicts.
- Shipments: create with optional carrier, tracking reference, and estimated delivery; progress through `PENDING -> SHIPPED -> DELIVERED`. No external carrier integration is implied under DEC-008.
- Runtime verification: not completed because no backend responded on configured `localhost:8080` and no ADMIN runtime credentials were available. The database was not edited and no state was forced. The requested end-to-end journey remains pending an available application runtime.
- Production build: `npm run build` — PASS.
- Lint: `npm run lint` — PASS; the configured linter exits successfully with React effect-pattern warnings in asynchronous loading pages. No test or E2E framework was added while DEC-011 and DEC-012 remain open.
- Backend/API blockers: none in the accepted Phase 4H discovery-read contract. Image ordering/primary mutation, order cancellation, and external carrier behavior are intentionally unavailable rather than frontend blockers.
- Remaining decisions: DEC-001, DEC-003, DEC-006, DEC-007, DEC-008, DEC-009, DEC-010, DEC-011, and DEC-012 retain their recorded status.

## Phase 4H correction pass — UX and image uploads

- Filter stability diagnosis: ADMIN Products, Inventory, Orders, and Categories expose pagination but their accepted contracts have no search/filter parameters or Search/Clear controls. ADMIN Custom Requests has one immediately-applied status select and no redundant Search/Clear actions. No unstable object dependencies or duplicate query-param writes were present. The six lint warnings identify loader invocation from effects; they are not repeated-action handlers and were not the source of the reported flashing. Unsupported filters were not invented.
- Product-image upload: frontend already used exact `POST /admin/products/{id}/images`, field `file`, browser-managed multipart boundaries. The backend upload root remained relative, making `transferTo(...)` target a different location from the directory created by NIO. Storage resolution is now absolute, normalized, and containment-checked. HTTP 413 receives a specific under-10-MB message while structured errors retain centralized normalization.
- Reference images: CUSTOMER and ADMIN details fetch authoritative protected `imageUrl` values as authenticated blobs, render responsive thumbnails with alt text and fallback, retain metadata, and offer safe full-size viewing. No URL is derived from `storageReference`.
- Product creation: successful creation navigates directly to the new product detail route with image-upload guidance and the corrected upload/display/delete section.
- Frontend build: `npm run build` — PASS. Lint: `npm run lint` — PASS with six investigated `react(set-state-in-effect)` warnings and no errors.
- Runtime verification: unavailable because no backend responded at `localhost:8080` and no ADMIN runtime credentials were available. Interactive browser workflows remain pending a runnable environment.

## Next recommended task

Begin Phase 4G — Admin Catalogue & Inventory. Keep DEC-001, DEC-002, DEC-003, DEC-004, DEC-006, DEC-011, and DEC-012 in their recorded states unless separately approved.
