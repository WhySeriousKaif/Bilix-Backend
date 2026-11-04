# Bilix Retail Data Platform — Data Engineering Implementation Plan

## Purpose

Build a separate data-engineering repository that consumes Bilix retail data and turns it into reliable analytics datasets. The POS application remains the transactional source system; the data platform demonstrates ingestion, storage, transformations, orchestration, quality, observability, and recovery.

Recommended repository name:

```text
bilix-data-platform
```

This plan will be reconciled with the college syllabus after the syllabus is provided. The project should satisfy course requirements without forcing every ecosystem tool into one pipeline.

## Portfolio statement

> Built an end-to-end retail data platform that ingests transactional sales and inventory data, processes batch and streaming events, maintains tested dimensional models, supports idempotent backfills, and serves analytics-ready datasets through an orchestrated and observable pipeline.

## Business use cases

The platform should answer concrete questions:

- Daily sales and order count by store, branch, and product.
- Net sales after refunds.
- Inventory movement and stockout trends.
- Product/category performance.
- Cashier and shift activity.
- Customer purchase frequency and value.
- Data-quality and pipeline-health status.
- ML-ready daily demand features.

## Architecture

Build in layers. Start with batch; add streaming only after batch correctness is demonstrated.

```text
Bilix PostgreSQL + synthetic event generator
                    |
          batch extract / CDC events
                    |
          raw object storage (MinIO/S3)
                    |
          Spark/Python validation and transforms
                    |
        curated Parquet tables / warehouse
                    |
             dbt dimensional models
                    |
       Metabase/Superset + ML feature exports

Airflow orchestrates batch jobs, tests, backfills, and alerts.
Kafka/Debezium provides the optional streaming path.
```

## Recommended learning stack

Use a coherent subset:

- Python and SQL.
- PostgreSQL as source and initially as analytical warehouse.
- MinIO locally or S3 in cloud for object storage.
- Parquet as the analytical file format.
- Airflow for batch orchestration.
- dbt for warehouse transformations, documentation, and tests.
- Spark for distributed/big-data transformations.
- Kafka plus Debezium for optional CDC/streaming.
- Metabase or Superset for BI.
- Docker Compose for the local platform.
- Data-quality checks through dbt tests plus one dedicated validation library if needed.

Do not add Spark, Kafka, Airflow, dbt, Flink, Hadoop, and multiple warehouses merely to list them on a resume. Every selected tool must solve a demonstrated problem.

## Phase 1 — Source contracts and reproducible data

### 1. Define source datasets

Start with:

- Orders.
- Order items.
- Products and categories.
- Stores and branches.
- Inventory movements.
- Refunds and refund items.
- Customers.
- Users/cashiers and shifts.

For each source define:

- Primary key.
- Business key.
- Event/update timestamp.
- Tenant/store/branch key.
- Expected nullability.
- Sensitive fields.
- Deletion/update semantics.
- Schema owner and version.

### 2. Generate realistic test volume

Create a deterministic synthetic-data generator with a seed. It should produce:

- Multiple stores and branches.
- Thousands or millions of orders depending on available hardware.
- Seasonality and weekend effects.
- Product/category variation.
- Refunds and cancelled transactions.
- Late-arriving and duplicate records for testing.
- Inventory movements and stockouts.

Label synthetic data clearly. Never claim it is real production data.

### 3. Protect the transactional application

- Data jobs must not perform heavy analytical queries against the live application database.
- Use incremental extraction by stable timestamp/ID or CDC.
- Use a read replica in a realistic cloud design.
- Remove or tokenize sensitive customer data before analytical storage.

## Phase 2 — Minimum viable batch pipeline

### 4. Raw ingestion

Implement a daily/hourly incremental extract:

```text
PostgreSQL -> partitioned raw Parquet -> object storage
```

Required behavior:

- Watermark/checkpoint tracking.
- Partitioning by ingestion date and useful event date.
- Source metadata: extraction time, source table, schema version, run ID.
- Retries without duplicated output.
- Manifest containing row count and checksum where practical.
- Quarantine for invalid records.

### 5. Orchestration with Airflow

Create a workflow with explicit tasks:

```text
source_check
  -> extract_incremental
  -> validate_raw
  -> transform_staging
  -> run_dbt_models
  -> run_quality_tests
  -> publish_metrics
```

Demonstrate:

- Scheduling.
- Dependencies.
- Retries and timeouts.
- Parameterized execution date/data interval.
- Backfill of a historical date range.
- Rerunning one failed task safely.
- Failure notification.
- Unit/import test for the DAG/workflow.

Airflow is an orchestrator for workflows with a clear start and end; it should invoke processing jobs rather than contain all transformation logic.

### 6. Dimensional warehouse model

Build a star schema:

Facts:

- `fact_sales_line` at one order-item grain.
- `fact_refund_line` at one refund-item grain.
- `fact_inventory_movement` at one movement grain.
- `fact_daily_inventory_snapshot` at product/branch/day grain.

Dimensions:

- `dim_date`.
- `dim_product`.
- `dim_category`.
- `dim_store`.
- `dim_branch`.
- `dim_customer` with sensitive columns excluded/tokenized.
- `dim_cashier`.
- `dim_payment_method` if useful.

Document the grain of every fact table. Implement surrogate keys and at least one Slowly Changing Dimension Type 2 example, such as product price/category or branch attributes.

### 7. dbt transformations and tests

Layers:

- Source definitions.
- Staging models that rename/type/clean source fields.
- Intermediate business models.
- Fact/dimension marts.
- Business aggregate models.

Tests:

- Unique and not-null keys.
- Referential relationships.
- Accepted enum values.
- Positive quantities where required.
- Refund quantity does not exceed sold quantity.
- Gross/net sales reconciliation.
- Freshness expectations.
- Custom test for tenant/store consistency.

Generate dbt documentation and lineage.

## Phase 3 — Scale and distributed processing

### 8. Spark processing

Implement one justified Spark job, for example rebuilding daily product/branch sales from raw Parquet.

Demonstrate:

- Explicit schemas instead of uncontrolled inference.
- Partition pruning.
- Join strategy awareness.
- Deduplication using business keys and event timestamps.
- Handling malformed records.
- Incremental output.
- Unit test against a small deterministic dataset.
- Comparison of output with the SQL/dbt implementation.

Do not claim big-data scale only because Spark runs locally. Report actual record count, runtime, and hardware honestly.

### 9. File layout and performance

Demonstrate:

- Parquet columnar storage.
- Appropriate partition keys, avoiding excessive tiny partitions.
- Compaction of small files.
- Schema evolution policy.
- Retention/lifecycle policy.
- Explain/query plan inspection.
- Before/after measurement for one optimization.

## Phase 4 — Optional streaming/CDC track

Add this only after the batch pipeline is complete.

### 10. CDC ingestion

Possible flow:

```text
Bilix PostgreSQL -> Debezium -> Kafka -> streaming consumer -> raw/curated storage
```

Demonstrate:

- Change events for inserts and updates.
- Event key and schema design.
- Consumer groups and offsets.
- At-least-once delivery assumptions.
- Idempotent sink/deduplication.
- Dead-letter/quarantine handling.
- Schema compatibility and evolution.
- Replay from a known offset or raw event archive.

### 11. Streaming transformation

Use Spark Structured Streaming or a simpler Kafka consumer for one use case:

- Near-real-time branch sales totals.
- Low-stock events.
- Refund anomaly feed.

Handle:

- Event time versus processing time.
- Late events and watermarking.
- Duplicate events.
- Checkpointing.
- Recovery after consumer restart.
- Reconciliation between streaming and batch totals.

## Phase 5 — Serving, quality, and operations

### 12. Analytics serving

Deliver:

- A BI dashboard for sales, refunds, inventory, and pipeline freshness.
- Parameterized SQL examples.
- A documented data dictionary.
- ML-ready daily product/branch demand table.

### 13. Data observability

Track:

- Pipeline success/failure and duration.
- Input/output row counts.
- Freshness and lateness.
- Null/invalid/duplicate rate.
- Schema changes.
- Reconciliation differences.
- Quarantine/dead-letter volume.

Include alerts and a runbook for stale data, schema breakage, and reconciliation failure.

### 14. Privacy and governance

Implement:

- Data classification.
- Customer-data minimization/tokenization.
- Role-based access design.
- Retention and deletion policy.
- Dataset ownership.
- Lineage/documentation.
- Audit trail for pipeline deployments.

## Twelve-week college-project schedule

### Weeks 1–2: design and data

- Finalize problem statement and source contracts.
- Create architecture and dimensional-model diagrams.
- Create deterministic synthetic data.
- Establish repository, tests, and Compose environment.

### Weeks 3–4: batch ingestion

- Incremental PostgreSQL extraction.
- Raw Parquet/object storage.
- Watermarks, manifests, retries, and quarantine.

### Weeks 5–6: warehouse and dbt

- Staging, facts, dimensions, SCD2.
- Data tests, reconciliation, documentation, and lineage.

### Week 7: Airflow

- Scheduled DAG, retries, backfill, failure path, and notification.

### Week 8: Spark

- One distributed transformation with partitioning and performance evidence.

### Weeks 9–10: streaming extension

- Kafka/Debezium CDC and one streaming use case if the batch system is stable.
- Otherwise use this time for stronger tests and reliability.

### Week 11: BI and observability

- Dashboard, freshness/quality metrics, alerts, and runbooks.

### Week 12: deployment and presentation

- Reproducible demo.
- Architecture and data-lineage diagrams.
- Failure/backfill demonstration.
- Final report, metrics, limitations, and interview notes.

## Minimum viable version if time is limited

Complete these before Kafka:

1. Reproducible synthetic retail data.
2. Incremental batch ingestion to partitioned Parquet.
3. Airflow orchestration with retries and backfill.
4. dbt star schema and meaningful quality tests.
5. One Spark transformation.
6. BI dashboard plus freshness and reconciliation metrics.
7. Docker Compose, tests, and documentation.

This is already a strong data-engineering internship project.

## Interview demonstrations

- Run the pipeline twice and prove idempotency.
- Inject a duplicate and show deduplication.
- Inject an invalid record and show quarantine.
- Backfill a missing date range.
- Change a dimension attribute and show SCD2 history.
- Stop and restart a task/consumer and show recovery.
- Reconcile warehouse net sales against the source.
- Explain partition selection using an execution plan.

## Definition of done

- A clean machine can start the platform from documented steps.
- Data lineage from source to dashboard is documented.
- Incremental reruns do not duplicate facts.
- Backfill and failure recovery are demonstrated.
- Data-quality tests fail on intentionally corrupted data.
- Source and warehouse totals reconcile within documented rules.
- Sensitive fields are excluded or protected.
- Tool selection is justified by requirements.

## Syllabus reconciliation checklist

When the college syllabus is provided, map every topic to:

```text
Syllabus topic:
Already covered by:
Required implementation artifact:
Required theory/explanation:
Gap:
Action:
```

Then revise this roadmap rather than adding duplicate tools.

## Primary documentation used

- Apache Airflow: <https://airflow.apache.org/docs/apache-airflow/>
- Airflow core concepts: <https://airflow.apache.org/docs/apache-airflow/stable/core-concepts/>
- Apache Spark documentation: <https://spark.apache.org/documentation.html>

