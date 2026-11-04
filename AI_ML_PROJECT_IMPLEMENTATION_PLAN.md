# AI/ML Portfolio Project Implementation Plan

## Recommendation

For AI/ML and AI-engineering roles, build a dedicated project rather than making forecasting another feature inside the already-large Bilix application.

Bilix demand forecasting is a valid classical ML/data-science project, especially after the data platform exists. It is not the best single project for every AI-engineering role because it may show forecasting without showing modern AI application engineering, evaluation, retrieval, safety, and model operations.

Recommended portfolio strategy:

1. Build one separate AI-engineering project as the primary AI project.
2. Treat Bilix demand forecasting as an optional second project or later integration.
3. Reuse engineering skills—APIs, tests, Docker, CI, monitoring—but keep the AI problem and repository independent.

## Primary project proposal

### Evidence-grounded document intelligence system

Build a system that ingests complex documents and answers questions with citations, evaluates retrieval and answers, and exposes operational metrics.

Example domains:

- College policies and academic regulations.
- Technical manuals and engineering documents.
- Legal/compliance policies using public documents.
- Research-paper exploration.

Choose a domain with documents you are legally allowed to use and with questions whose answers can be verified.

Possible repository name:

```text
evidence-document-intelligence
```

## Portfolio statement

> Built an evidence-grounded document intelligence system with structured ingestion, hybrid retrieval, cited responses, offline evaluation, experiment tracking, asynchronous processing, API deployment, and production monitoring.

## Phase 1 — Define the problem and evaluation set

Before choosing models, define:

- Target users.
- Document types.
- Questions the system should answer.
- Questions it must refuse or mark unsupported.
- Latency and cost constraints.
- Privacy requirements.

Create a versioned evaluation dataset:

- Representative documents.
- Questions.
- Expected answer facts.
- Expected supporting passages/documents.
- Unanswerable questions.
- Metadata-filtering cases.
- Adversarial/prompt-injection cases.

This dataset is the core of the project. Without it, model changes cannot be measured.

## Phase 2 — Reproducible ingestion

Implement:

- File upload or batch import.
- Document type validation.
- Text extraction with page/section metadata.
- OCR only when scanned documents require it.
- Cleaning without destroying important structure.
- Deterministic chunk IDs.
- Configurable chunking strategy.
- Content hashing and duplicate detection.
- Incremental re-indexing when a document changes.
- Failed-document quarantine and retry.
- Document/version status in a database.

Persist enough metadata to cite the original source and page/section.

## Phase 3 — Retrieval baseline

Start with measurable baselines:

1. Keyword/BM25 retrieval.
2. Dense vector retrieval.
3. Hybrid retrieval.
4. Optional reranking.

Measure:

- Recall@k.
- Precision@k where useful.
- Mean Reciprocal Rank or nDCG.
- Retrieval latency.
- Index size and ingestion time.

Compare strategies on the same evaluation dataset. Do not assume vector retrieval is automatically best.

## Phase 4 — Grounded answer generation

Implement:

- Prompt that restricts answers to retrieved evidence.
- Citations mapped to exact source chunks/pages.
- “Insufficient evidence” behavior.
- Context-length management.
- Output schema validation.
- Provider/model abstraction so experiments are repeatable.
- Token, latency, and estimated-cost recording.

Protect the system against document prompt injection by treating document text as untrusted content, separating instructions from evidence, and testing adversarial documents.

## Phase 5 — Evaluation

Evaluate retrieval and generation separately.

Generation metrics should cover:

- Factual correctness against expected facts.
- Citation correctness.
- Citation completeness.
- Faithfulness to retrieved context.
- Answer relevance.
- Correct refusal on unanswerable questions.
- Latency and cost.

Use a combination of:

- Deterministic checks.
- Human-reviewed sample set.
- Model-based evaluation only with documented rubrics and spot checks.

Create an experiment report comparing at least:

- Chunk sizes/overlap.
- Keyword versus dense versus hybrid retrieval.
- With/without reranking.
- At least two model or prompting configurations if affordable.

## Phase 6 — AI service engineering

Build:

- FastAPI or equivalent backend.
- PostgreSQL for document/job/feedback metadata.
- Vector index/store chosen from requirements.
- Background worker and queue for ingestion.
- Idempotent ingestion jobs.
- Authentication and per-user/workspace document access.
- Rate limiting and request limits.
- Streaming answer endpoint if useful.
- Structured errors and correlation IDs.

Minimum endpoints:

```text
POST /documents
GET  /documents/{id}
POST /documents/{id}/index
POST /query
POST /feedback
GET  /health
GET  /ready
```

## Phase 7 — Testing, deployment, and monitoring

Tests:

- Parser/chunking unit tests.
- Retrieval integration tests.
- Authorization tests.
- Citation mapping tests.
- Unanswerable/adversarial cases.
- Evaluation regression test with thresholds.
- Provider failures, timeouts, and retry behavior.

Deployment:

- Dockerized API and worker.
- Compose development environment.
- CI for lint, tests, build, and evaluation smoke test.
- Staging deployment.
- Secret management.
- Versioned prompt/config/model metadata.

Monitoring:

- Request count, latency, and error rate.
- Retrieval latency and empty-result rate.
- Model/provider failures.
- Token usage and cost.
- Citation/refusal/feedback rates.
- Ingestion failures and queue depth.
- Evaluation score changes per release.

## Phase 8 — User interface

Keep the UI focused:

- Upload and processing status.
- Document library.
- Question and response view.
- Clickable citations that open the source location.
- Feedback control.
- Admin/evaluation dashboard.

Do not spend most of the project on visual polish.

## Optional second project — Bilix demand forecasting

Build this after the Bilix data platform produces a trustworthy daily-demand table.

Problem definition:

- Forecast daily unit demand per product and branch.
- Compare against naive seasonal baselines.
- Use time-based validation—never random train/test splitting for forecasting.
- Report per-product/branch and aggregate performance.

Implementation:

- Reproducible feature pipeline.
- Lag, rolling-window, calendar, price, promotion, and stockout features.
- Naive baseline plus one statistical and one tree-based model where justified.
- MAE/WAPE/RMSE with business interpretation.
- Backtesting across multiple time windows.
- Model and dataset versioning.
- Prediction API/batch output.
- Drift and forecast-error monitoring.
- Explicit handling of stockouts, new products, and sparse series.

This is strongest as an integration:

```text
bilix-data-platform -> ML feature table -> forecasting pipeline -> prediction table/API
```

It should not be trained directly inside the React or Spring Boot checkout workflow.

## Recommended implementation order

1. Choose the document domain and collect permitted source documents.
2. Create the evaluation dataset before model experimentation.
3. Build deterministic ingestion and citation metadata.
4. Implement keyword retrieval baseline.
5. Add dense/hybrid retrieval and measure improvement.
6. Add grounded generation and refusal behavior.
7. Add evaluation automation and experiment tracking.
8. Build the API, background processing, auth, and feedback.
9. Add Docker, CI, staging, monitoring, and cost tracking.
10. Build the focused UI and record a demo.

## Minimum viable AI-engineering portfolio version

- At least 30–50 carefully reviewed evaluation questions.
- Reproducible document ingestion.
- Baseline and improved retrieval comparison.
- Cited answers and unsupported-question refusal.
- Retrieval and answer-quality evaluation.
- API, tests, Docker, CI, and deployed demo.
- Latency/cost metrics and documented limitations.

## Definition of done

- The system can explain which evidence supported an answer.
- Retrieval changes are evaluated rather than judged from a few demos.
- Unsupported questions do not silently hallucinate confident answers.
- Document access is isolated by user/workspace.
- Ingestion can be retried without duplicate index entries.
- Model/prompt/index versions are recorded.
- CI detects quality regression on a small stable evaluation suite.
- The README reports failures and limitations honestly.

## Resume bullet after completion

> Built and deployed an evidence-grounded document intelligence system with hybrid retrieval, source-level citations, asynchronous ingestion, automated quality evaluation, access isolation, and latency/cost monitoring.

## Decision summary

- **Primary AI-engineering project:** separate from Bilix.
- **Bilix demand forecasting:** good optional classical ML project after the data platform is reliable.
- **Do not:** add a forecasting chart to Bilix without baselines, time-based evaluation, model versioning, and monitoring.

