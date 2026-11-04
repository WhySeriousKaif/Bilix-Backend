# Bilix DevOps and Infrastructure Implementation Plan

## Purpose

Bilix is the real application workload. This roadmap turns it into a DevOps portfolio project by making its build, deployment, infrastructure, monitoring, security, and recovery reproducible.

The application repository and infrastructure repository have different responsibilities:

```text
bilix-pos
  Application source, tests, Dockerfiles, database migrations

bilix-infrastructure
  Cloud infrastructure, Kubernetes/Helm, GitOps, monitoring, runbooks
```

Create `bilix-infrastructure` only after the Docker/CI foundation works. Until then, keep the minimal delivery files with the application.

## Portfolio statement

> Designed a reproducible delivery platform for a React/Spring Boot/PostgreSQL application using containers, CI/CD, infrastructure as code, staged deployments, observability, secret management, backup validation, and rollback runbooks.

## Level 1 — Minimum DevOps foundation

This level is sufficient to support SDE applications and is the prerequisite for a dedicated DevOps portfolio.

### 1. Containerization

Implement:

- Multi-stage backend Dockerfile that runs tests/builds outside the runtime image.
- Frontend Dockerfile that builds Vite assets and serves them through a production web server.
- Non-root runtime users where practical.
- `.dockerignore` files.
- Pinned base-image versions or documented update policy.
- Container health checks.
- Runtime configuration instead of secrets baked into images.

Verification:

- Clean reproducible builds.
- Containers start without source code or local dependencies mounted.
- Images contain only necessary runtime artifacts.
- Vulnerability scan results are captured in CI.

### 2. Local multi-service environment

Create a Compose stack with:

- Frontend.
- Backend.
- PostgreSQL.
- Named database volume.
- Internal network/service discovery.
- PostgreSQL health check.
- Backend readiness dependency.
- Optional profiles for monitoring tools.

One documented command should start the complete development stack. Docker Compose is designed for defining and running multi-container applications with services, networks, and volumes.

### 3. Continuous integration

Pull-request workflow:

- Checkout code.
- Cache dependencies safely.
- Run frontend lint and build.
- Run backend unit and integration tests.
- Start PostgreSQL service/Testcontainers as required.
- Build both images.
- Scan dependencies and container images.
- Upload test reports or build artifacts.
- Fail the workflow on any required check.

Main-branch workflow:

- Build immutable images.
- Tag with Git commit SHA and semantic release tag when applicable.
- Push to GitHub Container Registry or another registry.
- Generate provenance/SBOM if feasible.

### 4. Application configuration and migrations

Implement:

- Development, test, staging, and production profiles.
- Flyway migrations executed exactly once per deployment.
- Secret values supplied by CI/cloud secret storage.
- No live credentials in Git, images, build logs, or frontend bundles.
- Startup validation for required production configuration.

### 5. One staging deployment

Minimum deployment should provide:

- HTTPS.
- Public frontend and protected backend/database network path.
- Managed PostgreSQL if affordable.
- Domain or stable deployment URL.
- Health/readiness check.
- Smoke test after deployment.
- Documented manual rollback to the previous image.

## Level 2 — Dedicated `bilix-infrastructure` repository

Recommended structure:

```text
bilix-infrastructure/
├── terraform/
│   ├── modules/
│   └── environments/
├── kubernetes/
├── helm/bilix/
├── argocd/
├── monitoring/
├── policies/
├── scripts/
├── runbooks/
└── README.md
```

### 6. Infrastructure as code

Use Terraform to provision a deliberately small environment:

- Network and subnets.
- Firewall/security groups.
- Container registry if required.
- Compute or managed Kubernetes.
- Managed PostgreSQL.
- Object storage for backups/artifacts.
- DNS and certificate prerequisites.
- Monitoring integration.

Required practices:

- Reusable modules without premature abstraction.
- Separate staging and production variable files/state.
- Remote state with locking when using shared cloud infrastructure.
- `fmt`, `validate`, security scan, and `plan` in pull requests.
- Reviewed plan before apply.
- Outputs documented without exposing secrets.

Terraform's core workflow is write -> plan -> apply; demonstrate all three rather than committing unused `.tf` examples.

### 7. Kubernetes deployment

Implement only after Compose and cloud deployment work.

Required resources:

- Frontend and backend Deployments.
- ClusterIP Services.
- Gateway API or Ingress for HTTPS routing.
- ConfigMaps for non-secret configuration.
- Secret integration for sensitive configuration.
- Readiness, liveness, and startup probes.
- CPU/memory requests and limits.
- Pod disruption and rolling-update settings.
- Migration Job.
- Horizontal Pod Autoscaler after metrics exist.

Do not deploy PostgreSQL as an ordinary application Pod for the portfolio production environment; prefer a managed database.

### 8. Helm packaging

Create one Bilix chart with:

- Default values.
- Staging values.
- Production values.
- Configurable image tags, replicas, resources, URLs, and ingress/gateway settings.
- Template validation and linting in CI.

Avoid copying complete Kubernetes manifests per environment.

### 9. GitOps deployment

Use Argo CD after manual Kubernetes deployments are understood.

Demonstrate:

- Git repository as desired deployment state.
- Automated staging synchronization.
- Controlled production promotion.
- Commit-SHA image updates.
- Drift detection.
- Rollback through Git/image version.

## Level 3 — Operations, security, and reliability

### 10. Observability

Metrics:

- Request rate, latency, and 4xx/5xx counts.
- JVM memory and garbage collection.
- Database pool saturation.
- Checkout success/failure.
- Inventory conflicts.
- Payment/refund failures when implemented.
- Pod/container CPU, memory, restart count, and availability.

Logs:

- Structured JSON logs.
- Correlation/request IDs.
- Central log collection.
- Explicit redaction of tokens, passwords, payment data, and personal data.

Dashboards and alerts:

- Service availability.
- p95 latency and error rate.
- Database capacity.
- Repeated checkout/payment failures.
- Container crash loops.
- Backup failure.

### 11. Deployment safety

Implement:

- Immutable versioned images.
- Rolling deployment.
- Post-deployment smoke tests.
- Automatic failure detection.
- Previous-version rollback.
- Database migration compatibility policy.
- Feature flags or backward-compatible changes for risky releases.

Demonstrate one controlled failed deployment and successful rollback.

### 12. Backup and disaster recovery

Implement:

- Scheduled PostgreSQL backups.
- Documented retention.
- Encrypted backup storage.
- Restore into an isolated environment.
- Measured restore time and data-loss window.
- Runbook for database failure and accidental deletion.

A backup that has never been restored is not verified.

### 13. Security and supply chain

Implement:

- Least-privilege cloud/service accounts.
- Dependency and container scans.
- Secret scanning.
- SBOM generation if practical.
- Protected main branch and required CI checks.
- Pinned actions and reviewed third-party actions.
- Network restrictions around PostgreSQL.
- TLS at public boundaries.
- Image and infrastructure update process.

## Verification portfolio

The project should show evidence, not only configuration files:

- Architecture diagram.
- CI/CD flow diagram.
- Link or screenshots of successful pipeline runs.
- Terraform plan example with secrets removed.
- Argo CD/Kubernetes deployment state if implemented.
- Grafana dashboard screenshots.
- Load/smoke test results.
- Rollback demonstration.
- Database restore demonstration.
- Incident and deployment runbooks.
- Cost estimate and teardown instructions.

## Recommended implementation order

1. Correct the existing backend Docker build so tests are not permanently skipped.
2. Add the frontend production image.
3. Add PostgreSQL Compose and health checks.
4. Add Flyway and environment profiles.
5. Build pull-request CI.
6. Publish commit-SHA images.
7. Deploy staging with HTTPS and secrets.
8. Add metrics, structured logs, dashboard, and alerts.
9. Add backup and tested restore.
10. Create `bilix-infrastructure` with Terraform.
11. Add Kubernetes and Helm only if targeting DevOps/platform roles.
12. Add GitOps and advanced deployment safety last.

## Mapping the “DevOps Heroes” course

The referenced 20-session folder is not present in the current workspace. When the session list is provided, classify each session as:

- **Implement:** directly produces a Bilix infrastructure artifact.
- **Practice:** important knowledge demonstrated through a lab or runbook.
- **Learn only:** valuable theory but unnecessary in the portfolio deployment.
- **Defer:** duplicates another tool or adds complexity without evidence.

For each session record:

```text
Session:
Core concept:
Bilix use case:
Artifact to produce:
Verification evidence:
Decision: Implement / Practice / Learn only / Defer
```

Do not attempt to force all 20 sessions into the system.

## Definition of done for a DevOps internship portfolio

- A push can build, test, publish, and deploy a versioned Bilix release.
- Infrastructure is reproducible from version-controlled configuration.
- Secrets are not committed or baked into images.
- Health, logs, metrics, and alerts expose failures.
- A previous release can be restored.
- A database backup has been restored successfully.
- The repository contains concise operational runbooks.
- Every major claim has visible evidence.

## Primary documentation used

- Docker Compose: <https://docs.docker.com/compose/>
- GitHub Actions package/image publishing: <https://docs.github.com/en/actions/tutorials/publish-packages>
- Terraform documentation: <https://developer.hashicorp.com/terraform/docs>
- Terraform core workflow: <https://developer.hashicorp.com/terraform/intro/core-workflow>
- Kubernetes concepts: <https://kubernetes.io/docs/concepts/>
- Kubernetes ingress guidance: <https://kubernetes.io/docs/concepts/services-networking/ingress/>

