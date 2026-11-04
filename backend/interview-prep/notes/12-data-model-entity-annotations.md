# Data Model and Entity Annotations — SQL and MongoDB

This note explains how Java classes become database models in Spring Boot and how relationships are represented.

## 1. Entity vs MongoDB document

```text
SQL database       → @Entity class
MongoDB            → @Document class
SQL table          → MongoDB collection
SQL row            → MongoDB document
```

The current Molla POS backend uses JPA entities and a SQL database. The MongoDB section at the end shows the equivalent annotations.

## 2. Basic JPA entity

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;
}
```

```text
@Entity       = this class is stored as a database table
@Table        = optional table name and table settings
@Id           = primary key
@GeneratedValue = database/application generates the ID
@Column       = column rules and database mapping
```

## 3. `@GeneratedValue` strategies

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

Common strategies:

| Strategy | Simple meaning | Common use |
|---|---|---|
| `IDENTITY` | Database auto-increment column generates the ID | PostgreSQL/MySQL identity or auto-increment columns |
| `SEQUENCE` | Database sequence generates the ID | PostgreSQL and Oracle; efficient for many inserts |
| `TABLE` | A separate table stores the next ID value | Portable but usually slower |
| `AUTO` | Provider chooses a suitable strategy | Convenient default; actual behavior depends on database/provider |

The project uses both `IDENTITY` and `AUTO` in different models:

```java
// User.java
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

```java
// Product.java or Order.java
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

Interview answer:

> `IDENTITY` lets the database generate an auto-increment ID. `SEQUENCE` uses a database sequence. `TABLE` uses a separate table and is less common. `AUTO` lets Hibernate choose. I select the strategy based on the database and project needs.

## 4. `@Column` options

```java
@Column(
    name = "email_address",
    nullable = false,
    unique = true,
    length = 150
)
private String email;
```

Common options:

```text
name       = database column name
nullable   = whether NULL is allowed
unique     = whether duplicate values are prohibited
length     = maximum string length for VARCHAR-like columns
insertable = include field in INSERT statements
updatable  = include field in UPDATE statements
```

Important distinction:

```text
@NotBlank / @Email = Java request validation
@Column(nullable = false, unique = true) = database constraint
```

Use both when the API and database should enforce the rule.

## 5. Enum mapping

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private UserRole role;
```

`EnumType.STRING` stores values such as:

```text
ROLE_STORE_ADMIN
ROLE_CASHIER
```

`EnumType.ORDINAL` stores numbers such as `0`, `1`, `2`. It is usually risky because changing enum order can change the meaning of old database rows.

For interview purposes:

> I prefer `EnumType.STRING` because the database value remains readable and is safer when enum values are reordered.

## 6. Relationships: the main idea

Relationships describe how rows in different tables are connected.

```text
One-to-one  = one record connects to one record
One-to-many  = one record connects to many records
Many-to-one  = many records connect to one record
Many-to-many = many records connect to many records
```

## 7. `@ManyToOne` in this project

Many users can belong to one store:

```java
// User.java
@ManyToOne
private Store store;
```

Many users can belong to one branch:

```java
// User.java
@ManyToOne
private Branch branch;
```

The database normally stores a foreign key such as `store_id` or `branch_id` in the `users` table.

Business meaning:

```text
One Store
    ├── Store Admin
    ├── Store Manager
    ├── Branch Manager
    └── Cashier
```

This is not the same as saying one store has only one user. It means each individual user points to one store, while the same store can be referenced by many users.

## 8. `@OneToMany`

One order can contain many order items:

```java
// Order.java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<OrderItem> orderItems;
```

Meaning:

```text
One Order → many OrderItem records
```

`mappedBy = "order"` means `OrderItem` owns the relationship through its `order` field. The foreign key is maintained on the many-side table.

`cascade = CascadeType.ALL` passes persistence operations from the order to its items.

`orphanRemoval = true` removes an item from the database when it is removed from the order collection, depending on the entity lifecycle.

## 9. `@OneToOne`

One-to-one means one row is associated with one row on the other side.

Example from the project:

```java
// Branch.java
@OneToOne(cascade = CascadeType.REMOVE)
private User manager;
```

The intended business meaning is:

```text
One Branch → one manager User
```

However, this does not automatically enforce every business rule by itself. The database schema and service logic must also prevent the same manager from being assigned incorrectly or multiple managers from being assigned.

If the requirement is “one Store has exactly one Store Admin,” that relationship should be modeled explicitly and backed by a unique constraint or service validation. In this project, users currently have `@ManyToOne Store`, so one store can have many users with different roles.

## 10. Relationship ownership and `mappedBy`

The side with the foreign key is usually the owning side.

```java
@ManyToOne
private Store store;
```

The inverse side can use:

```java
@OneToMany(mappedBy = "store")
private List<User> users;
```

```text
mappedBy = the Java field name on the owning side
```

It is not the database column name. It tells JPA which side controls the relationship.

## 11. Cascade types

```java
@OneToMany(cascade = CascadeType.ALL)
```

Common cascade operations:

```text
PERSIST  = save child when parent is saved
MERGE    = merge child when parent is merged
REMOVE   = delete child when parent is deleted
ALL      = all cascade operations
```

Use `REMOVE` and `ALL` carefully. Deleting a parent can delete related records.

## 12. Lifecycle annotations

The project uses lifecycle methods for timestamps:

```java
@PrePersist
public void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}

@PreUpdate
public void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

```text
@PrePersist = runs before a new entity is inserted
@PreUpdate  = runs before an existing entity is updated
@PostLoad   = runs after an entity is loaded
```

## 13. `@Transient`

```java
@Transient
private Double calculatedTotal;
```

This field exists in Java but is not stored as a database column. It is useful for calculated or temporary values.

## 14. `@ElementCollection`

The project uses this for simple values such as branch working days:

```java
@ElementCollection
private List<String> workingDays;
```

This creates a separate collection table for the values, but the values are not independent entities with their own ID.

Use `@Entity` for a real object with its own identity and lifecycle. Use `@ElementCollection` for a group of simple values owned by one entity.

## 15. Fetching relationships

```java
@ManyToOne(fetch = FetchType.LAZY)
private Store store;
```

```text
LAZY  = load related data only when accessed
EAGER = load related data immediately
```

Lazy loading can reduce unnecessary queries. It must be handled carefully outside the transaction, where the persistence context may already be closed.

## 16. MongoDB equivalents

MongoDB stores documents rather than relational tables and foreign-key joins.

```java
@Document(collection = "users")
public class UserDocument {

    @Id
    private String id;

    private String name;
    private String email;
}
```

Repository:

```java
public interface UserRepository
        extends MongoRepository<UserDocument, String> {
}
```

Common MongoDB annotations:

```text
@Document       = maps class to a MongoDB collection
@Id             = maps field to MongoDB _id
@Field("email") = custom document field name
@Transient      = do not persist this field
@DBRef          = reference another document (use carefully)
```

MongoDB relationship style is usually represented by either embedded data or an ID/reference:

```java
@Document("stores")
public class StoreDocument {
    @Id
    private String id;

    private String adminUserId;
}
```

MongoDB does not use JPA annotations such as `@Entity`, `@ManyToOne` or `@OneToMany`.

## 17. SQL relationship vs MongoDB design

```text
SQL/JPA
    Store table + User table
    foreign key store_id
    @ManyToOne / @OneToMany

MongoDB
    store document may embed users
    or store user IDs/references
    application decides how to load related data
```

SQL is useful when relationships, constraints and transactions are central. MongoDB is useful when document-shaped data and flexible schema are more important.

## 18. Interview answer

> In the SQL version of the project, `@Entity` maps a Java class to a table, `@Id` identifies the primary key, and `@GeneratedValue` controls ID generation. `@Column` defines nullability, uniqueness and column settings. Relationships use annotations such as `@ManyToOne`, `@OneToMany` and `@OneToOne`; for example, many users can belong to one store, while one order can contain many order items. `mappedBy` identifies the inverse side, and cascade controls whether operations propagate. For MongoDB, we use `@Document`, `@Id` and `MongoRepository`; relationships are represented by embedded documents or IDs rather than JPA relationship annotations.

## 19. Final memory shortcut

```text
@Entity                  = SQL table
@Id                      = primary key
@GeneratedValue          = generated ID strategy
@Column(nullable=false)  = database value is required
@Column(unique=true)     = no duplicate values
@ManyToOne               = many records point to one
@OneToMany               = one record owns many
@OneToOne                = one record points to one
mappedBy                 = inverse side field name
CascadeType.ALL          = propagate all lifecycle operations
@PrePersist              = before INSERT
@PreUpdate               = before UPDATE
@Transient               = not stored
@ElementCollection       = simple values in a collection table
@Document                = MongoDB collection
MongoRepository          = MongoDB operations
```

## 24. `equals()` in authorization checks

The screenshot contains logic like:

```java
boolean isAdmin = user.getRole()
    .equals(UserRole.ROLE_STORE_ADMIN);

boolean isManager = user.getRole()
    .equals(UserRole.ROLE_STORE_MANAGER);

boolean isSameStoreAdmin = user.equals(
    store.getStoreAdmin()
);
```

These two `equals()` calls are doing different comparisons.

### Comparing enum roles

```java
user.getRole().equals(UserRole.ROLE_STORE_ADMIN)
```

`UserRole` is an enum. Each enum constant is a single defined value, so this checks whether the user's role is exactly `ROLE_STORE_ADMIN`.

For enums, this is also commonly written as:

```java
user.getRole() == UserRole.ROLE_STORE_ADMIN
```

Both are value/constant comparisons for this enum. `==` is safe for enums because each enum constant is unique. If the role might be `null`, use a null-safe form:

```java
UserRole.ROLE_STORE_ADMIN.equals(user.getRole())
```

### Comparing two `User` entities

```java
user.equals(store.getStoreAdmin())
```

This does not compare role or every field. In the project's `User` entity, `equals()` is manually overridden to compare the database ID:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;
    return id != null && id.equals(user.id);
}
```

So the comparison means:

```text
Do these two User objects represent the same database user ID?
```

It can return `true` even when the objects were loaded in different places, because they represent the same row.

The corresponding `hashCode()` is:

```java
@Override
public int hashCode() {
    return getClass().hashCode();
}
```

This follows the JPA-safe pattern of not using a generated ID that may be `null` before persistence.

### Important current-project note

The current `CategoryServiceImp` no longer performs the old screenshot comparison directly. It delegates store authorization to:

```java
accessControlService.requireStoreAccess(user, store.getId());
```

That is safer because it checks access to the specific store, not merely whether the user has a store-admin role somewhere.

### Interview answer

> In the role check, `equals()` compares enum constants such as `ROLE_STORE_ADMIN`. In the user comparison, `User.equals()` is overridden to compare database IDs, so two separate Java objects representing the same database user are considered equal. This is different from the default object-reference comparison. In the current project, store access is centralized in `AccessControlService`, which checks the user's permission for the specific store.

## 20. `@Embeddable` and `@Embedded`

`@Embeddable` defines a reusable value object. Its fields are stored inside the parent entity's table instead of a separate table.

```text
@Embeddable = dependent reusable class
@Embedded   = put that class's fields into the entity table
```

### Embeddable class

```java
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    private String street;
    private String city;
    private String zipCode;
}
```

An embeddable class does not have its own `@Id`. It is not an independent table or entity.

### Entity using it

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Embedded
    private Address address;
}
```

The database table is conceptually:

```text
users
--------------------------------------
id | name | street | city | zip_code
```

JPA does not create a separate `addresses` table or an address foreign key. The address fields are flattened into the `users` table.

### Why use it?

Use an embeddable value object when a group of fields belongs to the parent and does not need its own identity.

```text
Address belongs to User
Address is not queried as an independent entity
Address has no separate primary key
```

This is different from `@OneToOne`, where both objects are independent entities and can have separate tables and IDs.

## 21. Reusing an embeddable class

If an entity embeds the same class twice, the default column names would collide:

```java
@Embedded
private Address homeAddress;

@Embedded
private Address workAddress;
```

Use `@AttributeOverrides` to rename the second set of columns:

```java
@Embedded
private Address homeAddress;

@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "work_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "work_city")
    ),
    @AttributeOverride(
        name = "zipCode",
        column = @Column(name = "work_zip_code")
    )
})
private Address workAddress;
```

The resulting columns can be:

```text
street | city | zip_code | work_street | work_city | work_zip_code
```

## 22. MERN comparison

Mongoose commonly stores a nested object inside one document:

```javascript
const userSchema = new mongoose.Schema({
  name: String,
  address: {
    street: String,
    city: String,
    zipCode: String
  }
});
```

The concepts are similar, but the storage shape differs:

```text
MERN/MongoDB
    nested Address object inside one document

Spring JPA/SQL
    Address fields flattened into the entity table
```

## 23. Interview answer

> `@Embeddable` defines a reusable value object, and `@Embedded` places its fields inside an entity. The embeddable class does not have its own ID or table. For example, an `Address` object can be embedded in `User`, resulting in `street`, `city` and `zip_code` columns in the `users` table. If the same embeddable is used twice, `@AttributeOverrides` gives the second set of fields different column names. In MongoDB, the similar idea is a nested object inside a document.

## 24. `@PrePersist` and `@PreUpdate`

These are JPA lifecycle callbacks. They run automatically before Hibernate performs database operations.

- `@PrePersist` runs before a new entity is inserted for the first time.
- `@PreUpdate` runs before an existing entity is updated.

They are useful for automatically setting audit fields such as `createdAt` and `updatedAt`.

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Import:

```java
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
```

Flow:

```text
new User → save() → @PrePersist → INSERT
existing User changed → save() → @PreUpdate → UPDATE
```

This prevents every service method from manually assigning timestamps. It is similar to a Mongoose `pre('save')` hook, but JPA provides separate callbacks for insert and update.
