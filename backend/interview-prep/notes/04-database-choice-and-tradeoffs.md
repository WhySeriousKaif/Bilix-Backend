# Database Choice and Trade-offs — POS System Interview Notes

## First: Which database does this project use?

The honest answer is:

> The project uses Spring Data JPA and Hibernate, so the application is written for a relational database. MySQL is the intended deployment database, while the current local configuration uses a file-based H2 database. The project also includes the PostgreSQL driver and can accept a PostgreSQL JDBC URL through environment configuration.

### Evidence from the current project

- `pom.xml` contains MySQL, PostgreSQL and H2 JDBC drivers.
- `DatabaseConfig` first checks Railway MySQL environment variables.
- `DatabaseConfig` also understands PostgreSQL and MySQL datasource URLs.
- `application.properties` currently defaults to a file-based H2 database.
- H2 is running in PostgreSQL compatibility mode locally.
- The backend README still documents MySQL as the main database.

Therefore, do not simply say, “I used PostgreSQL.” A more accurate answer is:

> I designed the persistence layer around a relational database using JPA. My intended production database was MySQL, and I used H2 locally for easy setup and persistent development data. Because I used JPA and standard relational mappings, the application also has some portability to PostgreSQL, although changing databases still requires proper integration testing and migration scripts.

## 1. Why does a POS system need a relational database?

A POS system contains strongly related data:

```text
Store
  └── Branch
        ├── Employees
        ├── Inventory
        ├── Orders
        │     └── Order Items
        ├── Refunds
        └── Shift Reports

Product ── Category
Order ── Customer
Order ── Cashier
Inventory ── Product + Branch
```

The relationships are important. For example:

- an order belongs to a branch;
- an order is processed by a cashier;
- an order contains multiple order items;
- every order item refers to a product;
- inventory connects a product with a branch;
- a refund refers to an existing order.

Relational databases are a natural fit because they provide:

- tables and relationships;
- primary and foreign keys;
- joins;
- transactions;
- unique and not-null constraints;
- strong consistency;
- mature reporting and aggregation with SQL.

## 2. The strongest reason: transactions and consistency

Consider placing an order:

```text
1. Validate the product exists
2. Check available stock
3. Reduce stock
4. Create the order
5. Create order items
6. Save payment information
```

These steps should behave like one unit.

If saving the order fails after stock has been reduced, the system should roll back the stock change. Otherwise, inventory becomes incorrect.

This is where an ACID transaction is useful.

### ACID in simple words

- Atomicity: either the complete operation succeeds or none of it is saved.
- Consistency: database rules remain valid before and after the operation.
- Isolation: simultaneous transactions should not corrupt one another.
- Durability: after a successful commit, the data should survive a crash.

### Important project observation

The database supports transactions, but the current `OrderServiceImpl.createOrder()` method is not marked with `@Transactional`.

For an interview, say:

> A production improvement would be to put order creation and inventory updates inside a transaction. That ensures the order and stock changes either commit together or roll back together.

Example:

```java
@Transactional
public OrderDto createOrder(OrderDto orderDto) {
    // validate stock
    // reduce stock
    // save order and order items
}
```

## 3. Why MySQL is a reasonable choice

MySQL is suitable for this project because:

- the data is relational and structured;
- it supports ACID transactions with InnoDB;
- it supports foreign keys, indexes and joins;
- it integrates easily with Spring Data JPA and Hibernate;
- it is widely supported by hosting providers;
- it has strong community support and documentation;
- it is common in business applications;
- it handles normal POS CRUD and reporting workloads well;
- Railway provides managed MySQL configuration, which influenced the deployment setup.

### MySQL trade-offs

- Complex analytics may eventually be better in a separate analytics system.
- Horizontal write scaling and sharding require additional design.
- Some advanced SQL and extension capabilities are stronger in PostgreSQL.
- Large reports can affect transactional queries if everything uses the same database.
- The application still needs correct indexes, queries and transaction boundaries; choosing MySQL alone does not guarantee performance or consistency.

## 4. Why use H2 locally?

The current local configuration uses H2 because it is convenient:

- it runs inside the Java application;
- developers do not need to install a database server;
- the application starts quickly;
- it works well for demos and local learning;
- the file-based mode keeps data between restarts;
- the H2 console makes local inspection easy.

### H2 trade-offs

- H2 is not MySQL or PostgreSQL.
- SQL syntax, data types, locking and constraint behavior can differ.
- PostgreSQL compatibility mode does not make H2 identical to PostgreSQL.
- A query can work in H2 and fail in the production database.
- H2 is usually not the right choice for a multi-user production POS deployment.
- File-based H2 is tied to one application instance and is unsuitable for normal horizontal scaling.

Interview answer:

> I used H2 for development convenience, not because it was the best production database. Production should use the same database engine used in integration testing—MySQL in the intended setup—to avoid environment-specific surprises.

## 5. MySQL vs PostgreSQL

Both are strong relational databases and either can support this POS system.

| Area | MySQL | PostgreSQL |
| --- | --- | --- |
| Basic POS CRUD | Very good | Very good |
| Transactions | Strong with InnoDB | Strong |
| Spring/JPA support | Excellent | Excellent |
| Ease of managed hosting | Very common | Very common |
| Complex SQL | Good | Often stronger and more feature-rich |
| JSON features | Good | Strong `JSONB` capabilities |
| Extensions | More limited | Rich extension ecosystem |
| Advanced indexing | Good | More advanced options |
| Strict SQL behavior | Can be more permissive depending on configuration | Generally strict and standards-oriented |
| Best reason here | Simple, familiar and aligned with Railway MySQL deployment | Strong choice if advanced querying, JSONB or PostgreSQL features are needed |

### Why not PostgreSQL?

PostgreSQL would not be a bad choice. The reason for MySQL is practical rather than absolute:

- the course/project originally targeted MySQL;
- the deployment configuration was written for Railway MySQL;
- the application does not currently require PostgreSQL-specific features;
- MySQL is sufficient for the expected transactional workload.

A good interview answer is:

> PostgreSQL could also handle this system. I selected MySQL because it met the transactional and relational requirements and matched the deployment environment. If the project needed advanced SQL, JSONB queries, richer indexing or PostgreSQL-specific extensions, PostgreSQL might be my preference.

## 6. Why not MongoDB?

MongoDB is useful when:

- documents have flexible or frequently changing structures;
- denormalized reads are more important than relationships;
- the application stores naturally document-shaped data;
- horizontal distribution is a primary requirement.

For the core POS data, MongoDB is not the first choice because:

- orders, products, branches, cashiers and inventory have important relationships;
- reporting often requires joins and aggregations across those relationships;
- stock updates and order creation require strong transactional behavior;
- relational constraints help prevent invalid references;
- duplicating product or branch data across documents can cause inconsistency.

MongoDB does support transactions, but using them heavily across many related collections may remove some of the simplicity that made a document database attractive.

Interview answer:

> MongoDB could store the data, but the domain is naturally relational. I preferred a relational database because the system needs joins, constraints, reporting and multi-step transactions around orders and inventory. MongoDB would be more attractive for flexible document data, such as raw activity events or variable product metadata.

## 7. Why not Redis as the main database?

Redis is excellent for:

- caching frequently read data;
- sessions;
- rate limiting;
- temporary carts;
- distributed locks;
- counters;
- short-lived tokens or OTPs;
- real-time leaderboards.

Redis should not replace the main relational database here because:

- the main data needs durable relational storage;
- joins and complex reports are not Redis's primary strength;
- modeling orders, items, refunds and relationships would be harder;
- memory is more expensive than disk-oriented relational storage;
- Redis persistence and failure behavior require careful configuration.

This project includes Redis support for caching, but Redis auto-configuration is currently disabled locally.

The intended design is:

```text
MySQL/PostgreSQL = source of truth
Redis = optional performance layer
```

## 8. Why not SQLite?

SQLite is useful for:

- mobile or desktop applications;
- embedded systems;
- single-user applications;
- offline storage;
- tests and prototypes.

It is not the best central database for this system because multiple branches and cashiers may write concurrently. A server database such as MySQL or PostgreSQL provides better concurrency, networking, user management, monitoring, backups and high-availability options.

SQLite could still be useful inside an offline cashier client, followed by synchronization with the central server.

## 9. Why not Cassandra or DynamoDB?

Cassandra and DynamoDB are useful when:

- extremely large scale is expected;
- high availability across regions is essential;
- access patterns are known in advance;
- denormalization is acceptable;
- horizontal scaling is more important than flexible relational queries.

They are not necessary for the current project because:

- the expected scale does not justify the extra complexity;
- the project needs relational reporting and transactions;
- queries and access patterns are still evolving;
- denormalizing all POS relationships would complicate development;
- operational and data-modeling complexity would be much higher.

At very large scale, a company might use more than one database instead of replacing SQL completely.

Example:

```text
Relational database → orders, payments and inventory
Redis              → cache and distributed locks
Kafka/event store  → sales events
Data warehouse     → large analytical reports
Search engine      → advanced product search
```

## 10. SQL vs NoSQL decision shortcut

Choose a relational database when you need:

- relationships;
- transactions;
- constraints;
- joins;
- consistent financial or inventory data;
- flexible reporting.

Consider a NoSQL database when you need:

- flexible document structures;
- very high horizontal scale;
- simple known access patterns;
- denormalized, high-speed reads;
- availability across many regions;
- a specialized data model such as key-value, document, graph or time series.

Do not choose SQL or NoSQL because one is “modern.” Choose based on data shape, query patterns, consistency requirements, scale and operational cost.

## 11. Important database improvements for this project

These are strong interview discussion points.

### A. Add transaction boundaries

Order creation, stock reduction and refund effects should use `@Transactional`.

### B. Prevent overselling

Two cashiers could read the same stock quantity at the same time and both complete a sale.

Possible solutions:

- optimistic locking with `@Version`;
- pessimistic row locking;
- an atomic conditional update such as “reduce quantity only when quantity is sufficient”;
- retry logic when an optimistic-lock conflict occurs.

### C. Use BigDecimal for money

The current entities use `Double` for monetary values. Binary floating-point can introduce rounding errors.

Prefer:

```java
private BigDecimal totalAmount;
private BigDecimal sellingPrice;
```

Also define currency, precision and rounding rules.

### D. Add proper indexes

Likely useful indexes include:

- user email;
- product SKU;
- product store ID;
- branch store ID;
- inventory branch ID and product ID;
- order branch ID and creation time;
- order cashier ID and creation time;
- refund branch ID and creation time.

An index speeds reads but adds storage and makes inserts/updates slightly more expensive. Index fields based on real query patterns, not every column.

### E. Add composite uniqueness

The project currently makes product SKU globally unique. If different stores may reuse the same SKU, a more suitable rule might be:

```text
UNIQUE(store_id, sku)
```

Inventory should also normally prevent duplicate rows for the same branch and product:

```text
UNIQUE(branch_id, product_id)
```

### F. Use schema migrations

The current project uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is convenient during development, but production systems should normally use versioned migrations with Flyway or Liquibase.

Benefits:

- repeatable deployments;
- reviewed schema changes;
- predictable upgrades;
- rollback planning;
- the same schema across environments.

### G. Test against the production database

H2 tests alone can hide database-specific problems. Integration tests can use Testcontainers to run a real MySQL or PostgreSQL container.

## 12. How would the database scale?

Start simple:

```text
Spring Boot application
        ↓
Primary relational database
```

Then scale based on evidence:

1. Optimize queries and add correct indexes.
2. Use connection pooling.
3. Add Redis for frequently read data.
4. Move expensive reports to background jobs or precomputed summaries.
5. Add read replicas for read-heavy dashboards.
6. Archive old transactional data when necessary.
7. Send analytics events to a warehouse.
8. Partition or shard only when one database can no longer meet measured needs.

### Read replica trade-off

- It reduces read load on the primary database.
- Replication can be slightly delayed.
- A dashboard may briefly show stale data.
- Critical operations such as checking stock should still read from a strongly consistent source.

### Sharding trade-off

Data could eventually be partitioned by store or tenant.

Benefits:

- distributes storage and write load;
- limits the amount of data per database.

Costs:

- cross-store reporting becomes harder;
- transactions across shards become difficult;
- routing, migrations and operations become more complex.

Sharding should not be the first solution for this project's current scale.

## 13. CAP theorem connection

CAP applies when a distributed system experiences a network partition.

- Consistency: every node returns the latest data.
- Availability: every request receives a response.
- Partition tolerance: the system continues operating despite communication failures between nodes.

For payment and inventory writes, this POS system usually favors correctness over accepting conflicting writes. Selling the same last product twice is worse than briefly rejecting or retrying a transaction.

However, not every feature needs the same choice:

- order/payment confirmation needs strong correctness;
- product browsing can tolerate slightly stale cached data;
- dashboards can often tolerate replication delay;
- audit events may be processed asynchronously.

This is a more mature answer than saying the entire system is simply “CP” or “AP.” Different operations can have different consistency needs.

## 14. Common interview questions

### Why did you choose a relational database?

Because the POS domain contains strong relationships and requires transactions, constraints, joins and reporting. Orders and inventory must remain consistent.

### Why MySQL instead of MongoDB?

The data is relational and transaction-heavy. MongoDB would require more denormalization and application-level consistency work for core orders and inventory.

### Why MySQL instead of PostgreSQL?

MySQL met the requirements, was familiar, and matched the intended Railway deployment. PostgreSQL would also be a valid choice and may be preferred if advanced SQL, JSONB or PostgreSQL extensions were needed.

### Why is H2 used?

For easy local development and demos. It is not intended to be the central production database.

### Can JPA make the application completely database-independent?

No. JPA reduces database-specific code, but SQL dialects, locking, data types, indexes, migrations and performance behavior still differ. Every supported database needs testing.

### How do you prevent two cashiers from selling the final item?

Use a transaction plus concurrency control, such as optimistic locking, pessimistic locking or an atomic conditional stock update.

### Why use a database transaction?

To ensure related changes—such as creating an order and reducing stock—commit or roll back together.

### What is an index?

An index is an additional data structure that helps the database find rows faster. It improves reads but consumes storage and adds work to writes.

### What is normalization?

Normalization separates data into related tables to reduce duplication and update inconsistencies. The trade-off is that reading combined data may require joins.

### When would you denormalize?

When measured read performance or reporting needs justify storing some repeated or precomputed data. Denormalization improves reads but increases consistency work.

### 401 versus 403—is that a database question?

No. `401 Unauthorized` means authentication is missing or invalid. `403 Forbidden` means the user is authenticated but lacks permission. The database may store users and roles, but the status codes come from the security layer.

### How would you back up the database?

Use automated snapshots, point-in-time recovery where supported, encrypted backups, retention policies and regularly tested restoration procedures. A backup is only trustworthy if restoration has been tested.

## 15. Interview answer to memorize

> I used a relational database because a POS system has strongly related and transaction-sensitive data: stores, branches, products, inventory, orders, order items, customers and refunds. MySQL was the intended production choice because it supports ACID transactions, foreign keys, joins and indexing, integrates well with Spring Data JPA, and matched the Railway deployment environment. I used file-based H2 locally for easy development, but I understand that H2 is not a production replacement for MySQL. PostgreSQL would also be a valid option, especially for more advanced SQL or JSONB requirements. I would not choose MongoDB as the primary database here because order creation and stock updates need strong relational consistency. For production, I would add transaction boundaries, concurrency control for inventory, BigDecimal for money, versioned migrations, appropriate indexes and integration tests against the actual production database.

## 16. Short answer for a quick interview

> I chose SQL because the POS domain is relational and needs strong transactions. MySQL was sufficient and matched the deployment environment. H2 was only for local development. PostgreSQL would also work, while MongoDB or Redis would be better used for specialized needs rather than as the source of truth for orders and inventory.

## Final memory shortcut

```text
POS data is relational
Orders + stock need transactions
MySQL is the intended production database
H2 is for local development
PostgreSQL is a valid alternative
MongoDB is better for flexible documents
Redis is a cache, not the source of truth
Scale with indexes and replicas before sharding
```
