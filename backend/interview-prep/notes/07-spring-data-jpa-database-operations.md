# Spring Data JPA — Database Operations Notes

## 1. MERN and Spring Boot architecture

In MERN, a Mongoose model usually contains both the data structure and database operations.

In Spring Boot, these responsibilities are separated:

```text
Entity     = describes the data structure
Repository = performs database operations
Service    = contains business logic
```

| Responsibility | MERN / Mongoose | Spring Boot / JPA |
|---|---|---|
| Data structure | `mongoose.Schema` | `@Entity` class |
| Database operations | `User.findById()`, `User.create()` | `UserRepository extends JpaRepository` |
| Business logic | Route handler or service | `@Service` class |

## 2. What does `JpaRepository<User, Long>` mean?

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

The two generic types mean:

```text
User = the entity/table managed by this repository
Long = the data type of User's @Id field
```

For example:

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

Because the ID is `Long`, the repository uses `JpaRepository<User, Long>`.

## 3. Finding a user by ID

### MERN

```javascript
const user = await User.findById(id);
```

### Spring Boot

```java
Optional<User> user = userRepository.findById(id);
```

`findById()` returns `Optional<User>` because the user may or may not exist.

```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("User not found"));
```

## 4. Built-in repository methods

`JpaRepository` already provides common operations:

```java
userRepository.save(user);
userRepository.findAll();
userRepository.findById(id);
userRepository.deleteById(id);
userRepository.count();
```

This means we do not need to write the basic SQL or implementation ourselves.

## 5. How does Spring create the implementation?

`UserRepository` is only an interface. We do not write its implementation class.

When Spring Boot starts:

```text
1. Spring scans repository interfaces.
2. It finds UserRepository.
3. It creates a proxy implementation at runtime.
4. That proxy uses JPA/Hibernate.
5. Hibernate generates and executes SQL for database operations.
```

So this code works even though there is no `UserRepositoryImpl` written by us:

```java
userRepository.findById(id);
```

## 6. Derived query methods

Spring can create a query from the method name:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
```

Spring reads `findByEmail` and understands that it should find a user whose `email` field matches the method argument.

Conceptually, it generates:

```sql
SELECT * FROM users WHERE email = ?;
```

More examples:

```java
List<User> findByRole(String role);

List<User> findByNameContaining(String text);

boolean existsByEmail(String email);

long countByRole(String role);
```

MERN comparison:

```javascript
User.findOne({ email });
User.find({ role });
User.exists({ email });
```

## 7. Complete request flow

```text
HTTP request
     ↓
@RestController
     ↓
@Service
     ↓
UserRepository
     ↓
JPA/Hibernate generates SQL
     ↓
Database
```

The controller should normally not contain database details. It calls the service, and the service calls the repository.

## 8. Complete example

### Entity

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
```

### Repository

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

`@RequiredArgsConstructor` creates the constructor for the final repository field, and Spring injects the repository through that constructor.

## 9. What does `@Repository` mean here?

`@Repository` marks a class as a database-access component and allows Spring to translate certain database exceptions.

For a Spring Data JPA repository interface, Spring detects it through repository scanning, so writing `@Repository` is often optional:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

The important part is extending `JpaRepository`.

## 10. Interview comparison

### MERN

```javascript
const user = await User.findById(id);
```

The Mongoose model directly performs the query.

### Spring Boot

```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("User not found"));
```

The repository performs the query, JPA/Hibernate creates the SQL, and the entity represents the database data.

Interview answer:

> `JpaRepository<User, Long>` tells Spring Data that this repository manages the `User` entity and that its primary key type is `Long`. It provides built-in CRUD methods such as `save`, `findById`, `findAll` and `deleteById`. Spring creates the repository implementation at runtime using a proxy. We can also define derived query methods such as `findByEmail`, where Spring reads the method name and generates the query. This is similar to Mongoose model methods, but Spring separates the entity from the repository.

## 11. Final memory shortcut

```text
@Entity                 = database data structure
@Id                     = primary key
JpaRepository<T, ID>    = database operations for entity T and ID type ID
findById()              = built-in lookup by primary key
findByEmail()           = query generated from method name
JPA/Hibernate           = converts repository calls into SQL
@Service                = business logic layer
```

## 12. MongoDB in Spring Boot

For a relational database such as PostgreSQL or MySQL, Spring commonly uses JPA/Hibernate.

For MongoDB, Spring uses **Spring Data MongoDB**.

The main replacements are:

```text
@Entity                    → @Document
JpaRepository<User, Long>  → MongoRepository<User, String>
table                      → collection
row                        → document
primary key                → MongoDB _id
```

## 13. MongoDB document class

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;
    private String email;
}
```

`@Document(collection = "users")` maps the class to the `users` collection.

`@Id` maps the Java field to MongoDB's `_id` field. The ID can be a `String` or an `ObjectId`-related type.

Constructors, getters and setters can be written manually or generated with Lombok.

## 14. MongoDB repository

```java
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository
        extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
}
```

`MongoRepository` provides methods such as:

```java
userRepository.save(user);
userRepository.findById(id);
userRepository.findAll();
userRepository.deleteById(id);
```

Spring can derive the email query from the method name. Conceptually, it searches for:

```json
{ "email": "kaif@example.com" }
```

## 15. MERN/Mongoose vs Spring Data MongoDB

| Concept | MERN / Mongoose | Spring Data MongoDB |
|---|---|---|
| Mapping style | ODM through Mongoose | Spring Data MongoDB mapping |
| Schema | `new mongoose.Schema(...)` | Java class with `@Document` |
| ID | Usually `ObjectId` in `_id` | `@Id` field mapped to `_id` |
| Basic queries | `User.find()`, `User.findById()` | `MongoRepository` methods |
| Complex queries | `User.aggregate(...)` | `MongoTemplate` or `@Query` |

MERN example:

```javascript
const user = await User.findOne({ email });
```

Spring example:

```java
Optional<User> user = userRepository.findByEmail(email);
```

## 16. `MongoTemplate` for complex queries

Repository method names are convenient for common queries. For aggregation pipelines or more complex queries, Spring provides `MongoTemplate`.

```java
Query query = new Query();
query.addCriteria(Criteria.where("name").is(name));

List<User> users = mongoTemplate.find(query, User.class);
```

We can also use an explicit query annotation:

```java
@Query("{ 'name': ?0 }")
List<User> findByName(String name);
```

MERN comparison:

```javascript
const users = await User.aggregate([
  { $match: { name } }
]);
```

## 17. Important execution differences

### Mongoose

```text
Define schema
        ↓
Create model with mongoose.model()
        ↓
Call model methods
```

### Spring Data MongoDB

```text
Create Java class with @Document
        ↓
Create repository extending MongoRepository
        ↓
Call repository methods
```

Spring automatically converts Java objects to BSON when saving and converts BSON documents back to Java objects when reading.

## 18. Switching from SQL to MongoDB

The service layer can remain mostly similar because both repositories provide common methods such as `save`, `findById` and `deleteById`.

Relational version:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

MongoDB version:

```java
public interface UserRepository
        extends MongoRepository<User, String> {
}
```

The repository type and entity mapping change, but the service can still call:

```java
userRepository.save(user);
userRepository.findById(id);
```

The service is not completely database-independent—relationships, transactions and complex queries may require changes—but basic CRUD code is very similar.

## 19. Interview answer

> For MongoDB, Spring Boot uses Spring Data MongoDB instead of JPA/Hibernate. We mark the Java class with `@Document`, use `@Id` for MongoDB's `_id`, and extend `MongoRepository<User, String>`. The repository provides common CRUD methods and can derive queries from method names such as `findByEmail`. This is similar to Mongoose models, but Spring separates the document mapping class from the repository. For complex queries or aggregation pipelines, we can use `MongoTemplate` or `@Query`.

## 20. Final memory shortcut

```text
@Document              = MongoDB document mapping
@Id                    = MongoDB _id
MongoRepository       = MongoDB CRUD operations
MongoRepository<T, ID> = entity type T and ID type ID
findByEmail()          = derived MongoDB query
MongoTemplate          = complex queries and aggregations
```

## 21. Custom repository queries

Spring Data JPA supports two common ways to define repository queries:

```text
1. Derived query method
2. JPQL query with @Query and @Param
```

## 22. Derived query method

The project has:

```java
List<Product> findByStoreId(Long storeId);
```

Spring reads the method name:

```text
findBy + StoreId
```

and creates the query automatically. Conceptually:

```sql
SELECT * FROM product WHERE store_id = ?;
```

No query string is written by the developer.

Other examples:

```java
Optional<Product> findBySku(String sku);
List<User> findByEmail(String email);
List<Product> findByNameContaining(String name);
```

Use derived methods for simple lookups whose method names remain readable.

## 23. `@Query` and `@Param`

The project uses a custom JPQL query for product search:

```java
@Query("""
    SELECT p FROM Product p
    WHERE p.store.id = :storeId
    AND (
        LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
    )
""")
List<Product> searchByKeyword(
        @Param("storeId") Long storeId,
        @Param("query") String query
);
```

### JPQL, not raw SQL

JPQL uses Java entity names and Java fields:

```text
Product p
p.store.id
p.name
p.brand
p.sku
```

It does not directly use database table and column names such as `products`, `store_id` or `product_name`. Hibernate translates the JPQL into database-specific SQL.

### Named parameters

The query contains:

```text
:storeId
:query
```

The method binds them with:

```java
@Param("storeId") Long storeId
@Param("query") String query
```

The string inside `@Param` must match the placeholder in the JPQL query.

The Java variable can have a different name:

```java
@Param("query") String keyword
```

Spring still binds `keyword` to `:query` because the annotation explicitly defines the connection.

### Case-insensitive contains search

```java
LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
```

Meaning:

```text
LOWER(...)                 = ignore letter case
CONCAT('%', :query, '%')   = add wildcards before and after input
LIKE                       = match the text pattern
```

If the input is `phone`, the pattern becomes approximately:

```text
%phone%
```

It can match `Phone`, `phone cover` or `smartphone`.

## 24. When to use each approach

| Approach | Use when | Main benefit |
|---|---|---|
| Derived method | Simple lookup by a few fields | No query string; quick to write |
| `@Query` + `@Param` | `AND`/`OR`, `LIKE`, functions, joins or custom logic | Full query control |
| `MongoTemplate` | MongoDB complex queries or aggregations | Flexible MongoDB query construction |

Interview answer:

> `findByStoreId` is a derived query. Spring parses the method name and generates the query automatically. `searchByKeyword` uses JPQL because it has a store condition, multiple OR conditions, wildcard matching and case conversion. `@Param` connects Java method arguments to named placeholders such as `:storeId` and `:query`. JPQL uses entity fields, and Hibernate converts it into SQL.

## 25. Why `findById` returns `Optional<User>`

```java
Optional<User> result = userRepository.findById(id);
```

`findById` may find a user, or it may find nothing. `Optional<User>` clearly represents both possibilities:

```text
User found     -> Optional contains the user
User not found -> Optional is empty
```

This is safer than returning `null`, which can later cause a `NullPointerException`.

### Recommended service code

```java
public User getUserById(Long id) throws UserException {
    return userRepository.findById(id)
            .orElseThrow(() -> new UserException("User not found"));
}
```

The flow is: search by ID, return the user if present, otherwise throw the application-specific exception.

### Common methods

```java
Optional<User> optionalUser = userRepository.findById(id);

optionalUser.isPresent();       // true when a user exists
optionalUser.isEmpty();         // true when no user exists
optionalUser.get();             // returns user; unsafe when empty
optionalUser.orElse(null);      // returns user or null
optionalUser.orElseThrow();     // returns user or throws an exception
```

Avoid this when possible:

```java
User user = userRepository.findById(id).get();
```

`get()` throws `NoSuchElementException` when the user is missing. `orElseThrow(...)` is clearer and allows a custom `UserException`.

### MERN comparison

```javascript
const user = await User.findById(id);

if (!user) {
  throw new Error("User not found");
}

return user;
```

Mongoose returns `null` when no document is found. Spring uses `Optional` to make the possibility of “no result” explicit in the method return type.

### MERN-style null check in Spring

You can also convert the `Optional` to `null` and check it manually:

```java
public User getUserById(Long id) throws UserException {
    User user = userRepository.findById(id).orElse(null);

    if (user == null) {
        throw new UserException("User not found");
    }

    return user;
}
```

This is similar to the MERN approach:

```javascript
const user = await User.findById(id);

if (user == null) {
  throw new Error("User not found");
}

return user;
```

Both approaches are valid. However, this is usually preferred:

```java
return userRepository.findById(id)
        .orElseThrow(() -> new UserException("User not found"));
```

`orElse(null)` introduces `null` and requires a separate check. `orElseThrow(...)` handles the empty case directly and keeps the code shorter.

### Important reminder

Standard `findById()` does not return `null` when a record is missing. It returns `Optional<User>`:

```java
Optional<User> result = userRepository.findById(id);
```

The `Optional` either contains a `User` or is empty. Use `orElse(null)` for a manual null check, or `orElseThrow(...)` to throw an exception directly.
