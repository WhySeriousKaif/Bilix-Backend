# Bilix POS — Implementation Guide and Next-Level Roadmap

This document is the working guide for improving the Bilix POS project. Future implementation work should use this roadmap unless the user explicitly changes the priority.

## Project goal

Turn Bilix into a production-oriented supermarket POS platform while keeping the code simple enough to explain in an interview.

The system currently has:

- Spring Boot backend with JWT authentication and role-based access.
- React/Vite frontend with Super Admin, Store Admin, Branch Manager, and Cashier panels.
- Store, branch, category, product, employee, customer, order, refund, inventory, report, and settings flows.
- Interview-preparation notes under `interview-prep/`.

## Working principles

1. Preserve the existing UI and user workflows unless a redesign is requested.
2. Prefer small, readable React components and plain CSS/layout logic.
3. Avoid adding unnecessary third-party UI components or abstractions.
4. Keep backend data dynamic; do not replace real API data with static values.
5. Treat the backend as the financial source of truth.
6. Validate authorization and input on the server, not only in the browser.
7. Explain every meaningful implementation in an interview note with a code snippet, API example, and trade-offs.
8. Verify changes with a build, relevant tests, and a manual user-flow check where possible.
9. Keep interview notes local unless the user explicitly asks to publish them.
10. Make one focused change at a time so it can be reviewed or reverted safely.

## Standard feature flow

For each new feature, follow this sequence:

1. Inspect the existing frontend page, API wrapper, backend controller, service, DTO, and repository.
2. Write down the current request/response flow before changing it.
3. Define authorization, validation, loading, empty, error, and success states.
4. Implement the backend contract first when new data or behavior is required.
5. Implement the simplest frontend state and API interaction.
6. Add pagination, filtering, and debouncing when the data size requires them.
7. Add or update an interview note in `interview-prep/`.
8. Run `npm run build` for frontend changes and the backend test/build command for backend changes.
9. Report the files changed, API calls involved, verification result, and known limitations.

## Production improvements to implement

### 1. Backend quality and security

- Replace hard-coded branch, cashier, and user IDs with authenticated identity/context.
- Use DTOs at API boundaries; do not expose JPA entities directly.
- Add consistent validation with clear 400 responses.
- Add a global `@RestControllerAdvice` error format.
- Centralize JWT parsing and security rules.
- Add role and ownership checks for every store/branch operation.
- Validate money and refund amounts on the server.
- Use database transactions for orders, inventory, and refunds.
- Add audit fields such as `createdAt`, `updatedAt`, and actor/user IDs.
- Use soft deletion where historical orders must remain readable.

### 2. Frontend reliability

- Centralize API errors and display useful toast/empty/error states.
- Replace fallback mock production data with explicit error states.
- Add route-level loading boundaries.
- Add reusable native components only when duplication becomes real.
- Keep form state controlled and validate before submission.
- Prevent duplicate submits with loading/disabled states.
- Keep confirmation dialogs for destructive actions such as logout, delete, refund, and end shift.

### 3. Data scalability

Add backend pagination and server-side filtering to large lists:

```http
GET /api/orders/branch/1/paged?page=0&size=20&sortBy=createdAt&direction=desc
```

Apply the same pattern to products, customers, employees, refunds, and transactions. Search should be debounced on the frontend and executed by the backend for large datasets.

### 4. Testing

- Backend unit tests for services and validation.
- Controller tests for status codes and authorization.
- Repository/integration tests for order, refund, and inventory transactions.
- Frontend tests for search, filters, forms, confirmation dialogs, and failed API calls.
- End-to-end tests for login → POS order → refund → shift close.

## Machine-learning roadmap

Machine learning should be added only after reliable transactional data, authentication, and reporting exist.

### First useful ML features

1. **Sales forecasting:** predict demand by product, branch, and day.
2. **Low-stock prediction:** estimate when inventory will reach reorder level.
3. **Product recommendations:** recommend frequently bought-together products.
4. **Anomaly detection:** flag unusual refunds, discounts, or cashier activity.
5. **Customer segmentation:** group customers by buying frequency and spend.

### Recommended ML architecture

```text
POS transactions
      ↓
Backend event/data export
      ↓
Clean feature tables
      ↓
Python training job
      ↓
Versioned model artifact
      ↓
Prediction API
      ↓
Spring Boot / React dashboard
```

Start with an offline Python training script and a small prediction endpoint. Do not put model-training logic inside React or inside a normal order request.

### ML interview explanation

Explain the difference between:

- **Features:** date, branch, product, quantity, price, promotion, and past sales.
- **Label:** the value being predicted, such as tomorrow's quantity.
- **Training:** learning patterns from historical data.
- **Inference:** using the trained model to produce a new prediction.
- **Evaluation:** measuring error on data the model did not train on.

Every ML result should show its timestamp, model version, and confidence/accuracy information.

## DevOps roadmap

### Local development

- Keep frontend and backend environment variables documented in `.env.example`.
- Use separate development database and credentials.
- Provide one command or documented sequence to run frontend and backend.
- Add health endpoints and a clear backend startup check.

### Continuous integration

On every pull request:

1. Install dependencies.
2. Run frontend lint and build.
3. Run backend tests and build.
4. Run security/dependency checks.
5. Publish test/build artifacts and fail on errors.

### Deployment

```text
Git push
   ↓
CI checks
   ↓
Build frontend and backend images
   ↓
Push versioned artifacts
   ↓
Deploy to staging
   ↓
Smoke tests
   ↓
Production approval/deployment
```

Use Docker, a managed database, HTTPS, secret storage, database migrations, structured logs, and a rollback strategy. Never commit JWT secrets, passwords, API keys, or production credentials.

### Complete DevOps implementation track

The project implementation must eventually cover these major DevOps concepts. They are deliberately ordered from local development to production operations.

#### 1. Docker

Create separate images for the React frontend and Spring Boot backend.

Deliverables:

- `frontend/Dockerfile`
- `backend/Dockerfile`
- `.dockerignore` files
- Environment variables passed at runtime, not baked into images
- Health checks for backend readiness

The image should be reproducible: the same commit should produce the same application artifact.

#### 2. Docker Compose

Create a `docker-compose.yml` for local development containing:

- Frontend container
- Backend container
- MySQL/PostgreSQL container
- Persistent database volume
- Internal service networking
- Environment configuration
- Health-check-based startup ordering

Example service flow:

```text
browser → frontend → backend → database
                         ↓
                    optional Redis
```

Compose is for running the complete stack on a developer laptop. It is not a replacement for production Kubernetes.

#### 3. Container registry

Push versioned images to a registry such as Docker Hub, GitHub Container Registry, or a cloud registry.

```text
bilix/frontend:git-sha
bilix/backend:git-sha
```

Never deploy an untracked `latest` image as the only production version.

#### 4. CI/CD

Add a GitHub Actions pipeline that runs on pull requests and main-branch pushes:

```text
Pull request
  → install dependencies
  → lint
  → frontend build/test
  → backend test/build
  → dependency/security scan

Main branch
  → build Docker images
  → tag with commit SHA
  → push to registry
  → deploy staging
  → smoke test
  → approval or automated production deployment
```

The pipeline must fail if tests, builds, or security checks fail. Deployment credentials belong in GitHub Actions secrets or an external secret manager.

#### 5. Kubernetes

Kubernetes should manage containers rather than manually creating individual Pods. We define desired state through resources:

- `Deployment` for frontend and backend replicas
- `Service` for stable internal/network access
- `ConfigMap` for non-secret configuration
- `Secret` or external secret provider for credentials
- `Ingress` for HTTPS and domain routing
- `PersistentVolumeClaim` for stateful storage where required
- `HorizontalPodAutoscaler` for scaling
- `Job` for database migrations or one-time tasks

```text
Deployment → ReplicaSets → Pods
Service → routes traffic to healthy Pods
Ingress → exposes HTTP/HTTPS routes
```

Pods are replaceable runtime units. Do not store important data inside a Pod filesystem.

#### 6. Helm

Use Helm charts when the Kubernetes manifests need reusable values for development, staging, and production:

```text
helm/bilix/
├── Chart.yaml
├── values.yaml
├── values-staging.yaml
├── values-production.yaml
└── templates/
```

Helm keeps image tags, replica counts, domains, and resource limits configurable without copying manifests.

#### 7. GitOps

Use Git as the source of truth for deployment configuration. Application code and infrastructure configuration are reviewed through pull requests.

The GitOps flow is:

```text
Developer push
  → CI builds and publishes image
  → deployment repository updates image tag
  → GitOps controller detects the change
  → cluster reconciles to Git
```

Argo CD is the recommended GitOps controller to learn for this project. It continuously compares the desired state in Git with the cluster and reports drift.

#### 8. Terraform / Infrastructure as Code

Terraform should provision infrastructure, not application business logic. It can create:

- Cloud network and subnets
- Kubernetes cluster/node pools
- Managed database
- Container registry
- Object storage for product/profile images
- DNS and TLS prerequisites
- Monitoring integrations

Recommended structure:

```text
terraform/
├── providers.tf
├── main.tf
├── variables.tf
├── outputs.tf
├── environments/
│   ├── staging.tfvars
│   └── production.tfvars
└── modules/
    ├── network/
    ├── kubernetes/
    └── database/
```

Use remote state with locking, review plans before applying, and never commit Terraform state containing secrets.

#### 9. Secrets and configuration

Separate configuration by environment:

```text
development ≠ staging ≠ production
```

JWT secrets, database passwords, payment keys, Cloudinary credentials, and registry credentials must come from a secret manager or protected CI/Kubernetes secret. `.env.example` may contain names and safe sample values only.

#### 10. Observability: Prometheus and Grafana

Add metrics and dashboards for:

- Request count and latency
- 4xx/5xx errors
- Login failures
- Order and refund counts
- Payment failures
- Inventory update failures
- Database connection pool usage
- Pod CPU and memory

Prometheus collects metrics; Grafana visualizes them. Add alerts for sustained 5xx errors, unavailable Pods, high latency, low database capacity, and unusual refund spikes.

#### 11. Logs and tracing

Use structured JSON logs with request/correlation IDs. Never log passwords, JWTs, payment secrets, or full card data. Add distributed tracing later when frontend, backend, payment, and ML services need request-level diagnosis.

#### 12. Deployment safety

The production track must include:

- Rolling deployments
- Readiness and liveness probes
- Resource requests and limits
- Database migration strategy
- Backups and restore testing
- Canary or blue/green deployment knowledge
- Rollback to the previous image tag
- Disaster recovery documentation

#### 13. DevOps interview deliverables

For interview preparation, explain these concrete artifacts:

1. Why Docker solves environment differences.
2. Why Compose is useful locally.
3. Why a Kubernetes Deployment creates and replaces Pods.
4. Why Services and Ingress are separate.
5. Why Helm values differ by environment.
6. How GitOps reconciles the cluster from Git.
7. What Terraform manages versus what Kubernetes manages.
8. How CI differs from CD.
9. How Prometheus/Grafana detect failures.
10. How secrets, rollback, and database backups protect production.

#### DevOps completion order

```text
Dockerfiles
  → Docker Compose
  → CI build/test pipeline
  → Container registry
  → Kubernetes manifests
  → Helm chart
  → Terraform infrastructure
  → GitOps with Argo CD
  → Prometheus/Grafana monitoring
  → autoscaling, rollback, backups, and security hardening
```

### Observability

- Health/readiness checks.
- Structured request logs with correlation IDs.
- Error tracking for frontend and backend.
- Metrics for request latency, failed requests, orders, refunds, and stock updates.
- Alerts for repeated 5xx responses, payment failures, and unusual refund activity.

## Interview preparation format

For each feature note, answer:

1. What problem does this feature solve?
2. Which URL and HTTP method are used?
3. What request body/query parameters are sent?
4. Which controller/service/repository handles it?
5. What DTO comes back?
6. How does the frontend store and render the result?
7. What are loading, empty, error, and unauthorized states?
8. How would pagination, caching, validation, and tests improve it?
9. What could go wrong in production?

## Current learning sequence

Continue in this order:

1. Finish cashier interview notes: POS terminal, order history, returns/refunds, customers, shift summary, settings.
2. Add cashier pagination and server-side order/customer search.
3. Harden backend validation, errors, authorization, and refund transactions.
4. Add automated tests for the main POS flows.
5. Add Docker and CI checks.
6. Add reporting data exports and an analytics data model.
7. Build the first ML feature: daily sales forecasting.
8. Add model evaluation, monitoring, and a dashboard explanation.

## Definition of done

A feature is complete when it has:

- Working backend API and authorization.
- Dynamic frontend data with loading/error/empty states.
- Validation and safe handling of destructive actions.
- Tests or a documented verification step.
- An interview note with a concise explanation and example.
- No secrets committed to the repository.

## Highest-priority commercial production work

This is the practical top-ten list to complete after the cashier pagination work. These items are more important than adding ML immediately because a commercial POS must first protect money, inventory, users, and historical records.

### 1. Payment and refund idempotency

Every payment and refund request should have a unique idempotency key. If a cashier double-clicks or the network retries a request, the backend must return the original result instead of creating a second payment or refund.

### 2. Inventory concurrency safety

Use database transactions and row locking when selling or restoring stock. Two cashiers must not be able to sell the same final unit. Stock changes should be auditable and never become negative.

### 3. Authentication and authorization hardening

Remove fallback test users from production. Use short-lived access tokens, refresh tokens, password reset, login rate limiting, account lockout, and server-side ownership checks for every store and branch resource.

### 4. Audit logs

Record who created orders, processed refunds, changed stock, edited customers, changed permissions, and reversed payments. Include actor, timestamp, branch, action, and affected record.

### 5. Payment reconciliation

Store gateway transaction IDs, verify webhooks server-side, handle pending/failed/reversed payments, and reconcile gateway records with POS orders. Never trust only a frontend payment-success message.

### 6. Production error handling and observability

Add a consistent backend error response, frontend error boundaries, structured logs, request IDs, health checks, metrics, and alerts for payment, refund, inventory, and server failures.

### 7. Database performance

Add indexes for product SKU/name, customer name/email, order date, refund date, and other searchable/sortable columns. Add aggregate endpoints for dashboard totals instead of calculating totals from loaded pages.

### 8. Automated tests and CI

Test refunds, payments, stock concurrency, pagination boundaries, permissions, webhooks, duplicate clicks, empty pages, and last pages. Run frontend build/lint and backend tests automatically on every pull request.

### 9. Network resilience and cashier recovery

Add retry for safe requests, offline indicators, draft-cart persistence, recovery after refresh, and safe handling of interrupted payments. If the business requires offline selling, add a controlled offline queue rather than silently pretending a request succeeded.

### 10. Backups, migrations, and disaster recovery

Use versioned database migrations, scheduled backups, tested restore procedures, separate environments, secret management, health checks, and a documented rollback process.

## What comes after the commercial foundation

Only after the ten items above are reliable should the ML/device roadmap become a production feature. The existing ML roadmap remains valid:

```text
Reliable POS transactions
      ↓
Clean historical data
      ↓
Sales forecasting
      ↓
Low-stock prediction
      ↓
Product recommendations
      ↓
Refund/cashier anomaly detection
      ↓
Customer segmentation
```

For devices, the practical future work is:

- Barcode scanner and keyboard-input reliability.
- Receipt printer and print-failure recovery.
- Cash drawer/device status handling.
- Payment-terminal integration and reconciliation.
- Device registration and branch assignment.
- Device heartbeat and health monitoring.
- Safe retry when a device or network disconnects.

ML and device integrations should consume trusted backend events. They should not be allowed to change orders, payments, refunds, or inventory directly without going through the same authorized backend services.
