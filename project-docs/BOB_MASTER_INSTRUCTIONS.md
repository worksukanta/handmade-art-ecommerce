# BOB MASTER INSTRUCTIONS

## Project

Handmade & Custom Artwork E-Commerce Platform
IBM Technical Training Capstone Project

## Technology Stack

Backend:

* Java
* Spring Boot

Frontend:

* React

Database:

* PostgreSQL

## Authoritative Project Documents

The following approved documents define the system and must be treated as the source of truth:

1. Software Requirements Specification (SRS)
2. MVP Scope Document
3. Requirements & Use Case Specification
4. System Design Document
5. Database Design & ERD
6. REST API Specification
7. UI/UX and Frontend Page Specification
8. Test Strategy and Test Case Specification

Do not invent functionality that is not supported by these approved documents.

## Core Rule

Implement the approved design.

Do not redesign the application unless an actual specification conflict or implementation blocker is discovered.

If a requested implementation conflicts with an approved specification, stop that part of the implementation and clearly report the conflict.

Do not silently modify requirements, database relationships, API contracts, workflows, roles, or business rules.

## Scope Control

Do not introduce new:

* Business features
* User roles
* Database entities
* API endpoints
* Status values
* Workflows
* External integrations
* Frameworks
* Dependencies

unless they are required by the approved specifications or explicitly approved during development.

Out-of-scope and SHOULD HAVE functionality must not be implemented as mandatory MVP functionality.

## Development Approach

Development must be incremental and module-based.

For each task:

1. Read the supplied task prompt.
2. Read DEVELOPMENT_STATUS.md.
3. Read relevant approved specifications supplied for the task.
4. Inspect the existing source code before changing it.
5. Determine what already exists.
6. Implement only the requested scope.
7. Preserve existing working behavior.
8. Add or update relevant tests.
9. Compile/build the affected project.
10. Run relevant tests.
11. Report the result.
12. Update DEVELOPMENT_STATUS.md when requested.

Do not attempt to implement unrelated future modules.

## Existing Code Rule

Never assume that a class, endpoint, table, DTO, service, repository, component, configuration, or dependency does not exist.

Inspect the repository first.

Do not create duplicate implementations.

Prefer modifying or extending the approved existing implementation where appropriate.

## Backend Rules

Maintain clear Spring Boot separation where applicable:

Controller
→ Service
→ Repository
→ Database

Use DTOs for API boundaries according to the approved REST API specification.

Do not expose persistence entities directly through REST APIs unless explicitly approved.

Use backend validation for business-critical rules.

The backend is authoritative for:

* Authentication
* Authorization
* Ownership
* Product availability
* Inventory
* Pricing
* Order totals
* Payment state
* Workflow state transitions

Frontend validation is supplementary only.

## Security Rules

Authorization must be enforced by Spring Boot.

Frontend route guards are not security controls.

CUSTOMER users must not access ADMIN operations.

Ownership must be enforced for customer-owned resources such as:

* Profile
* Addresses
* Cart
* Orders
* Payments
* Custom artwork requests
* Uploaded reference images

Do not expose passwords, authentication secrets, raw card data, CVV, PIN, or other sensitive payment credentials.

## Database Rules

Use PostgreSQL as defined by the approved database design.

Respect:

* Primary keys
* Foreign keys
* Unique constraints
* NOT NULL constraints
* Approved relationships
* Approved enum/status values
* Transaction boundaries
* Inventory consistency

Do not modify the database model merely to simplify implementation.

If an approved schema cannot support an approved workflow, report the conflict instead of silently changing the schema.

## REST API Rules

Follow the approved REST API specification exactly for:

* HTTP method
* Path
* Authentication
* Authorization
* Request DTO
* Response DTO
* Status codes
* Query parameters
* Business behavior

Do not create alternate endpoints simply because they are easier to implement.

Use `/api/v1` as the approved API base path.

## Workflow Rules

State transitions must follow approved lifecycle rules.

Do not allow arbitrary status changes.

Important workflows include:

Ready-made purchase:

Catalogue
→ Cart
→ Address
→ Checkout Validation
→ Order Creation
→ Payment
→ Order History

Custom Artwork:

Custom Request
→ Reference Image
→ Admin Review
→ Quotation
→ Customer Approval/Rejection
→ Advance Payment
→ Production
→ Shipping/Delivery

Only actions valid for the current state should be allowed.

## Testing Rules

Use the approved Test Strategy and Test Case Specification as the testing baseline.

Backend tests should use the appropriate combination of:

* JUnit 5
* Mockito
* Spring Boot Test
* MockMvc
* PostgreSQL test environment

Frontend testing tools must follow the actual frontend setup.

Do not mark a task completed if its required implementation does not compile.

Relevant tests should be executed before declaring a task completed.

If a test cannot be implemented because an approved decision remains unresolved, report it rather than inventing expected behavior.

## Open Decision Rule

When implementation reaches an unresolved design decision:

1. Do not guess.
2. Check DECISION_LOG.md.
3. If a decision already exists, follow it.
4. If no decision exists, report it as:

DECISION REQUIRED

Include:

* Issue
* Relevant specification
* Available options
* Implementation impact
* Recommended option

Do not continue implementation of the affected behavior until the decision is supplied.

## Dependency Rule

Do not add dependencies unnecessarily.

Before adding any dependency:

* Explain why it is required.
* Check whether existing dependencies already solve the problem.
* Prefer standard Spring Boot / React ecosystem functionality.

Avoid overengineering.

This is a technical training capstone, not a large enterprise production platform.

## Git Version Control Rules

Git version control must be used throughout development.

The project uses a simple checkpoint-based workflow.

### Branching

The `main` branch represents the latest reviewed and accepted implementation.

Development tasks may be performed on short-lived task branches such as:

`phase-2b-identity-model`

`phase-2c-catalogue-inventory`

`phase-3-authentication`

Avoid unnecessary complex branching strategies.

### Commit Scope

Each completed development task or sub-phase should result in a focused Git commit.

Do not combine unrelated modules into the same commit.

Prefer meaningful commit messages such as:

`feat: implement identity and address database model`

`feat: add catalogue and inventory persistence`

`feat: implement JWT authentication`

`test: add authentication integration tests`

`fix: prevent inventory overselling`

`docs: update development status`

### BOB Git Restrictions

BOB may inspect Git status and Git diff when useful.

BOB must NOT:

* force push
* rewrite Git history
* run `git reset --hard`
* delete branches
* merge branches
* rebase branches
* push to remote repositories
* commit automatically unless explicitly instructed in the current development task

BOB should leave completed code in the working tree and provide the Task Completion Report.

The developer will review and commit the accepted changes.

### Before Starting a Task

Where practical:

1. Working tree should be clean.
2. Previous accepted task should already be committed.
3. Development should occur on the intended task branch.

If unrelated uncommitted changes already exist, report them before modifying those files.

### After Completing a Task

Report:

* Files created
* Files modified
* Tests executed
* Build result
* Recommended commit message

Do not mark Git operations as completed unless they were actually executed.

### Safety Principle

Never use destructive Git commands to solve implementation problems.

Existing developer work must be preserved.


## Code Quality

Code should be:

* Readable
* Modular
* Consistent
* Testable
* Appropriately documented
* Appropriate for the approved architecture

Avoid premature abstraction.

Avoid unnecessary design patterns.

Avoid creating utility layers without a clear need.

## Error Handling

Use consistent API error handling according to the approved REST API specification.

Validation, authorization, resource-not-found, conflict, and business-rule errors should produce appropriate responses.

Do not leak stack traces or internal implementation details through API responses.

## Task Scope Rule

Every development prompt defines a bounded task.

Do not continue into the next module automatically.

When the requested task is complete, stop and provide a completion report.

## Required Completion Report

At the end of every development task, provide:

### TASK COMPLETION REPORT

Task:
Status: COMPLETED / PARTIALLY COMPLETED / BLOCKED

Files Created:

* ...

Files Modified:

* ...

Requirements Implemented:

* ...

APIs Implemented:

* ...

Database Changes:

* ...

Tests Added:

* ...

Tests Executed:

* ...

Tests Passed:

* ...

Tests Failed:

* ...

Build Status:

* ...

Specification Conflicts:

* None / details

Decisions Required:

* None / details

Known Issues:

* None / details

Recommended Next Task:

* ...

Do not claim completion when compilation or critical tests fail.

## Development Status Rule

DEVELOPMENT_STATUS.md is the persistent handoff document between development sessions.

At the end of a successful task, update it with:

* Current phase
* Current module
* Completed task
* Completed modules
* APIs implemented
* Database status
* Test status
* Known issues
* Pending decisions
* Recommended next task

Keep it concise.

Do not turn DEVELOPMENT_STATUS.md into a detailed development diary.

## Final Principle

When uncertain:

Approved specification

> existing implementation
> development task
> assumption

Never choose an assumption when the approved documents can answer the question.
