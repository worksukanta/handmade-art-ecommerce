# Codex Repository Instructions

## Source of truth

- Treat the approved project documents in `project-docs/` as authoritative.
- The approved REST API Specification defines all frontend/backend contracts. Do not invent endpoints.
- The approved UI/UX & Frontend Page Specification defines pages, navigation, fields, and user interactions.
- Treat the existing backend implementation as frozen. Do not modify backend code unless a genuine contract defect is found and the user explicitly authorizes the change.
- Follow the backend request and response DTO contracts exactly.

## Frontend rules

- Build a React frontend only.
- Before selecting libraries, inspect the existing `frontend/` directory and package configuration.
- Do not add frameworks or libraries unless the requested work requires them and the approved design permits them.
- Keep components modular without overengineering.
- Separate page, reusable component, API/service, routing, and cross-cutting concerns.
- Centralize the API base URL/configuration and use a shared service layer for backend calls.
- Handle authentication consistently through shared auth state, route guards, and API request/response handling.
- Treat the backend as authoritative for security, prices, totals, inventory, ownership, and workflow state.
- Client-side validation and route guards improve UX; they are never security controls.
- Never store passwords, raw payment-card data, CVV, PIN, or other sensitive payment details.
- Match the approved backend request/response DTOs exactly; do not infer extra fields or capabilities.

## Coding workflow

Before implementation:

1. Inspect the relevant approved specification.
2. Inspect the existing code and configuration.
3. Implement only the requested bounded task.
4. Run the appropriate frontend build, tests, and lint commands when configured.
5. Report changed files, verification performed, and any issues or contract mismatches.

## Git safety

Codex may inspect `git status` and `git diff`.

Unless explicitly instructed, Codex must not:

- commit;
- merge;
- rebase;
- push;
- reset;
- clean;
- delete branches.
