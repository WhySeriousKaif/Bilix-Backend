# Bilix POS — Minimum SDE Internship Implementation Plan

## Purpose

This is the minimum implementation required to present Bilix as a serious software-engineering internship project. The goal is not to add every possible feature. The goal is to demonstrate correctness, backend design, security, testing, and the ability to finish and deploy a working system.

## Portfolio statement

> Bilix POS is a multi-tenant retail platform built with React, Spring Boot, and PostgreSQL. It supports role-based store operations, transactional checkout, concurrency-safe inventory, refunds, pagination, automated testing, and containerized deployment.

Do not claim production scale, production-grade reliability, or real users unless those claims are supported by evidence.

## Bare-minimum scope

Complete these six milestones before expanding into ML, Kubernetes, hardware, or more UI modules.

### 1. Close the existing pagination work

The existing cashier pagination is enough for the first portfolio release. Do not paginate every screen before starting correctness work.

Required:

- Customer, order-history, and refundable-order lists use server-side pagination.
- Search resets the page to zero.
- Page, size, search, and sort values are validated.
- Empty, loading, failed, first-page, and last-page states work.
- Store/branch authorization applies to paginated endpoints.
- At least one controller or integration test covers page boundaries.
- The pagination learning guide matches the real API.

Later backlog:

- Products and employees when their data becomes large.
- Refunds and transactions when reports move to server-side querying.
- Stores, branches, and categories only if their expected size justifies it.

### 2. Implement atomic checkout

This is the main SDE feature.

Business invariants:

1. The server calculates item prices and totals.
2. The authenticated user determines cashier, store, and branch.
3. Branch inventory is the single stock source.
4. Stock never becomes negative.
5. Order creation, stock deduction, and stock history succeed or roll back together.
6. Two cashiers cannot sell the same final unit.
7. Every stock change has a business reason and reference.

Minimum implementation:

- Add a dedicated `CheckoutRequest` that contains product IDs and quantities, not authoritative prices or totals.
- Convert monetary values used in checkout to `BigDecimal` and database decimal columns.
- Consolidate stock under branch-level `Inventory`; deprecate `Product.quantity`.
- Add a unique constraint on `(branch_id, product_id)`.
- Add a database constraint preventing negative inventory.
- Add `@Transactional` to the checkout service boundary.
- Lock inventory rows with `PESSIMISTIC_WRITE` before checking and deducting stock.
- Lock product rows in deterministic product-ID order to reduce deadlock risk.
- Create immutable order-item price/name/SKU snapshots.
- Create an append-only `InventoryMovement` record for each sale and refund.
- Return `409 Conflict` for insufficient stock or a checkout conflict.

Minimum tests:

- Successful checkout calculates the server total and deducts stock.
- Insufficient stock rolls back the complete order.
- One invalid line rolls back every valid line in the cart.
- A product from another store cannot be purchased.
- A client cannot control price, total, cashier, branch, or final status.
- Two concurrent checkout attempts for one remaining unit produce one success and one conflict.
- Refund restores the same branch inventory and creates a refund movement.

### 3. Complete authorization boundaries

Required:

- Every store/branch resource performs an ownership check in the service layer.
- Cashiers can access only their assigned branch.
- Branch managers can access only their assigned branch.
- Store managers/admins can access only branches belonging to their store.
- Super admins have explicitly defined system-wide permissions.
- Authorization failures return `403`; validation failures return `400`.
- Do not accept tenant identity from the client when it can be derived from authentication.

Minimum tests:

- Cross-store resource ID substitution is rejected.
- Cross-branch resource ID substitution is rejected.
- Cashier cannot call employee-management or inventory-adjustment operations.
- Store manager cannot modify a different store.

### 4. Add a credible automated test suite

Required layers:

- Unit tests for pricing and validation rules.
- Service integration tests for checkout, refunds, and inventory.
- Controller authorization tests using Spring Security test support.
- Repository test for the locking query.
- One frontend test for duplicate submit or failed checkout behavior.
- One end-to-end happy path if time permits: login -> add cart -> checkout -> verify order.

Use PostgreSQL through Testcontainers for concurrency and locking tests. H2 is acceptable for lightweight tests but must not be the only evidence for PostgreSQL transaction behavior.

### 5. Add minimum delivery engineering

Required:

- Backend and frontend Dockerfiles.
- Docker Compose for frontend, backend, and PostgreSQL.
- Flyway migrations; stop depending on `ddl-auto=update` for portfolio deployment.
- `.env.example` with safe variable names and no secrets.
- GitHub Actions workflow running frontend lint/build and backend tests/build.
- Backend health endpoint.
- One reachable staging deployment.

### 6. Make the work reviewable

Required repository evidence:

- README with problem statement, architecture, roles, local setup, and demo credentials.
- Architecture diagram showing browser -> API -> database and checkout transaction.
- API examples for login, checkout, pagination, and refund.
- Test instructions and a screenshot/link showing CI passing.
- A short concurrency demonstration: two cashiers attempt to purchase the final unit.
- Known limitations and future-work section.

## Recommended implementation order

1. Finish and verify the current cashier pagination slice.
2. Add PostgreSQL development/test configuration and Flyway.
3. Define checkout invariants and failing tests.
4. Consolidate branch inventory and monetary types.
5. Implement the transactional checkout and row locking.
6. Update refunds to use the same inventory source.
7. Finish ownership/authorization tests.
8. Add Docker Compose and CI.
9. Deploy one staging environment.
10. Polish README, diagrams, demo, and resume bullets.

## Definition of done

The minimum SDE portfolio release is complete when:

- A new developer can run the stack from documented steps.
- CI passes from a clean checkout.
- Checkout cannot oversell the last item under the concurrency test.
- A failed checkout leaves no partial order or inventory change.
- Cross-tenant access tests pass.
- Pagination retrieves only the requested database page.
- The application is deployed and demonstrable.
- The README explains trade-offs and limitations honestly.

## Intentionally deferred

- Full payment reconciliation and webhooks.
- Complete offline selling.
- Kubernetes, Helm, Terraform, and GitOps.
- Hardware integration.
- Machine learning.
- Pagination on every small administrative table.
- Microservice decomposition.

These are later improvements, not blockers for applying to SDE internships.

## Resume bullet after completion

> Built and deployed a multi-tenant POS platform using React, Spring Boot, and PostgreSQL; implemented transactional checkout with row-level inventory locking, server-side pricing, role/ownership authorization, server-side pagination, and automated concurrency tests.

