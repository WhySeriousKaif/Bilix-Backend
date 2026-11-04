# Lombok Annotations — Easy Revision Notes

Lombok helps us remove repeated Java code, also called boilerplate code.

Instead of writing constructors, getters, setters and `equals()`/`hashCode()` manually, we can use annotations. Lombok generates that code during compilation.

## 1. `@NoArgsConstructor` and `@AllArgsConstructor`

### Why use them?

- `@NoArgsConstructor` creates an empty constructor.
- `@AllArgsConstructor` creates a constructor with all fields as parameters.

### Without Lombok

```java
public class User {

    private Long id;
    private String name;

    // No-args constructor
    public User() {
    }

    // All-args constructor
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

### With Lombok

```java
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String name;
}
```

Easy memory:

```text
NoArgsConstructor  = User()
AllArgsConstructor  = User(id, name)
```

These annotations are especially useful for JPA entities because frameworks often need an empty constructor to create objects.

## 2. `@Getter` and `@Setter`

### Why use them?

They create getter and setter methods for the fields.

Getters read values. Setters change values.

### Without Lombok

```java
public class User {

    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### With Lombok

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private Long id;
    private String name;
}
```

Easy memory:

```text
Getter = read a field
Setter = update a field
```

We can also put the annotation on one field if we want a getter or setter only for that field.

## 3. `@EqualsAndHashCode`

### Why use it?

By default, Java may compare objects by their memory reference. Two separate objects can contain the same values but still be treated as different objects.

`@EqualsAndHashCode` generates:

- `equals()` — checks whether two objects should be considered equal.
- `hashCode()` — creates a hash value used by `HashSet`, `HashMap` and other hash-based collections.

### Without Lombok

```java
import java.util.Objects;

public class User {

    private Long id;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;
        return Objects.equals(id, user.id)
            && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

### With Lombok

```java
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class User {

    private Long id;
    private String name;
}
```

Easy memory:

```text
equals()   = compare objects
hashCode() = support hash-based collections
```

Both methods should use the same fields. If two objects are equal, they must have the same hash code.

## 4. `@Data`

`@Data` is a shortcut annotation. It combines:

- `@Getter`
- `@Setter`
- `@ToString`
- `@EqualsAndHashCode`
- `@RequiredArgsConstructor`

Example:

```java
import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
}
```

Use `@Data` when all of these generated methods are suitable for the class. For entities, be careful with generated `equals()`, `hashCode()` and `toString()` when relationships or sensitive fields are present.

## 5. `@Value`

`@Value` is useful for immutable objects.

Immutable means that after the object is created, its values should not change.

```java
import lombok.Value;

@Value
public class ProductSummary {
    Long id;
    String name;
}
```

It generally creates final fields, getters, an all-args constructor, `equals()`, `hashCode()` and `toString()`. It does not create normal setters.

Easy memory:

```text
@Data  = convenient mutable data class
@Value = convenient immutable data class
```

## 6. Interview answer

> Lombok annotations reduce boilerplate Java code. `@NoArgsConstructor` and `@AllArgsConstructor` generate constructors. `@Getter` and `@Setter` generate accessor methods. `@EqualsAndHashCode` generates value-based equality and hash methods. `@Data` bundles commonly used annotations, while `@Value` is used for immutable objects. Lombok generates the code during compilation; it does not change the basic Java concept.

## 7. Final memory shortcut

```text
@NoArgsConstructor   = empty constructor
@AllArgsConstructor  = constructor with every field
@Getter              = read methods
@Setter              = update methods
@EqualsAndHashCode   = equality and hash methods
@Data                = common mutable data methods
@Value               = immutable data object
```

## 8. `equals()` and `hashCode()` — interview concept

Every Java class inherits these methods from `Object`.

### Default behavior

Without overriding them, `equals()` usually checks **reference equality**. Two objects containing the same data can still be different objects:

```java
User u1 = new User(1L, "Kaif");
User u2 = new User(1L, "Kaif");

System.out.println(u1 == u2);       // false
System.out.println(u1.equals(u2)); // false by default
```

`==` asks whether both variables point to the same object. It does not compare field values.

### Overriding `equals()`

Overriding `equals()` changes the comparison to **value equality**:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;
    return Objects.equals(id, user.id)
        && Objects.equals(name, user.name);
}
```

Now two `User` objects with the same `id` and `name` can be considered equal. This affects operations such as:

```java
users.contains(new User(1L, "Kaif"));
users.remove(new User(1L, "Kaif"));
```

### Overriding `hashCode()`

`hashCode()` returns an integer based on the fields:

```java
@Override
public int hashCode() {
    return Objects.hash(id, name);
}
```

The most important Java rule is:

> If two objects are equal according to `equals()`, they must return the same `hashCode()`.

This rule is required by `HashSet`, `HashMap` and other hash-based collections.

```java
Set<User> userSet = new HashSet<>();
userSet.add(new User(1L, "Kaif"));
userSet.add(new User(1L, "Kaif"));

// With both methods overridden, the duplicate is not added.
System.out.println(userSet.size()); // 1
```

How a hash collection works:

```text
1. hashCode() finds a bucket.
2. equals() checks the exact object inside that bucket.
```

If `equals()` and `hashCode()` are inconsistent, `HashSet` or `HashMap` may store duplicates or fail to find an object that appears equal.

### Short comparison

```text
equals()   = should these two objects be considered equal?
hashCode() = which bucket should this object use?
```

### Lombok connection

```java
@EqualsAndHashCode
public class User {
    private Long id;
    private String name;
}
```

Lombok generates both methods using the selected fields. This is why `@EqualsAndHashCode` is useful, but for JPA entities we should choose fields carefully—especially generated IDs, relationships and mutable fields.

### Interview answer

> `equals()` compares objects for logical value equality, while `hashCode()` produces the hash used by hash-based collections. If two objects are equal, their hash codes must be equal. Therefore, whenever I override `equals()`, I also override `hashCode()`. Lombok's `@EqualsAndHashCode` generates both methods for me.

## 9. `@Autowired` and dependency injection

`@Autowired` tells Spring to inject a managed bean into another class.

In simple words:

> Spring creates and manages the object for us, so we do not need to create it with `new`.

### Without dependency injection

```java
public class UserController {

    // The controller creates its own dependency.
    private UserService userService = new UserService();

    public void getUser() {
        userService.fetchUserDetails();
    }
}
```

This creates tight coupling. The controller decides exactly which service implementation to use, and replacing it with a mock in a unit test becomes harder.

### Field injection with `@Autowired`

```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user")
    public void getUser() {
        userService.fetchUserDetails();
    }
}
```

Spring searches its application context for a matching bean, such as a class marked with `@Service` or `@Component`, and places that object in the field.

### Constructor injection

```java
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

When a class has only one constructor, Spring automatically uses it for dependency injection. An explicit `@Autowired` is not required. This behavior is provided by Spring Framework 4.3 and later.

The explicit form is also valid:

```java
@Autowired
public UserController(UserService userService) {
    this.userService = userService;
}
```

### Lombok shortcut

`@RequiredArgsConstructor` creates a constructor for all `final` fields. Spring then uses that generated constructor.

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
}
```

This is still constructor injection. Lombok only writes the constructor during compilation.

### Why constructor injection is preferred

```text
Field injection       = dependency can be hidden or changed later
Constructor injection = dependency is clear and can be final
```

Constructor injection is usually preferred because:

- Dependencies are visible in the constructor.
- `final` fields can be immutable.
- The object cannot be created without required dependencies.
- Unit tests can pass a mock directly without starting Spring.

### Comparison

```text
new UserService()               = manual creation, tightly coupled
@Autowired field                = Spring injects it, but field injection
                                  is harder to test
constructor injection           = clear and test-friendly
@RequiredArgsConstructor        = Lombok-generated constructor injection
```

### Interview answer

> `@Autowired` enables dependency injection. Spring finds a matching bean from its application context and injects it into the class. Field injection works, but constructor injection is preferred because dependencies are explicit, fields can be final, and unit testing is easier. If there is only one constructor, Spring injects it automatically, and Lombok's `@RequiredArgsConstructor` can generate that constructor for final fields.

## 10. `@RequiredArgsConstructor`

`@RequiredArgsConstructor` generates a constructor only for:

1. Fields marked `final`.
2. Fields marked `@NonNull`.

It ignores normal non-final fields.

### Without Lombok

```java
@RestController
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private String optionalConfig;

    public UserController(
            UserService userService,
            EmailService emailService) {

        this.userService = userService;
        this.emailService = emailService;
    }
}
```

### With Lombok

```java
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private String optionalConfig;
}
```

Lombok generates the same constructor for the two `final` fields during compilation. `optionalConfig` is ignored because it is not final.

### Why it is useful in Spring

```text
final dependency
        ↓
Lombok generates constructor
        ↓
Spring sees the single constructor
        ↓
Spring injects the required beans
```

Benefits:

- Avoids field injection such as `@Autowired private UserService userService`.
- Makes dependencies clear in the class definition.
- Allows dependencies to remain `final`.
- Makes unit testing easier because dependencies can be passed directly.
- Automatically includes a new dependency when a new `final` field is added.

Spring Framework 4.3 and later automatically uses a single constructor, so `@Autowired` is normally not needed on the generated constructor.

### Constructor annotation comparison

```text
@NoArgsConstructor
    = public User() {}
    = useful for JPA entities and deserialization

@AllArgsConstructor
    = constructor with every field
    = useful for DTOs and test setup

@RequiredArgsConstructor
    = constructor with final and @NonNull fields
    = useful for Spring services and controllers
```

### Interview answer

> `@RequiredArgsConstructor` generates a constructor for all final and `@NonNull` fields. In Spring Boot, this supports constructor-based dependency injection. I mark required dependencies as final, Lombok generates the constructor, and Spring automatically injects the matching beans. This is cleaner and easier to test than field injection.

## 11. `@PathVariable`

`@PathVariable` extracts a value directly from the URL path and gives it to a controller method.

### Matching names

If the placeholder name and Java variable name are the same, the name inside the annotation can be omitted:

```java
// GET /api/users/10
@GetMapping("/api/users/{id}")
public ResponseEntity<UserDto> getUserById(
        @PathVariable Long id) {

    return ResponseEntity.ok(userService.getUserById(id));
}
```

Spring sees `{id}` and connects it to the Java parameter `id`.

### Different names

If the names are different, specify the URL placeholder explicitly:

```java
// GET /api/users/10
@GetMapping("/api/users/{userId}")
public ResponseEntity<UserDto> getUserById(
        @PathVariable("userId") Long id) {

    return ResponseEntity.ok(userService.getUserById(id));
}
```

This tells Spring:

```text
URL {userId} → Java variable id
```

### Multiple path variables

```java
// GET /api/users/42/orders/99
@GetMapping("/api/users/{userId}/orders/{orderId}")
public ResponseEntity<OrderDto> getUserOrder(
        @PathVariable Long userId,
        @PathVariable("orderId") Long currentOrderId) {

    return ResponseEntity.ok(
        orderService.findOrder(userId, currentOrderId)
    );
}
```

Here `{userId}` matches `userId`, while `{orderId}` is explicitly mapped to `currentOrderId`.

### MERN comparison

```text
Express route:  app.get('/api/users/:id', ...)
Spring route:   @GetMapping("/api/users/{id}")
```

```javascript
const id = req.params.id;
```

```java
public ResponseEntity<UserDto> getUserById(
        @PathVariable Long id) { ... }
```

Renaming in Express:

```javascript
const userId = req.params.id;
```

Renaming in Spring:

```java
@PathVariable("id") Long userId
```

### Interview answer

> `@PathVariable` extracts a value from a URL path and passes it to a controller method. If the URL placeholder and Java parameter have the same name, the annotation can omit the name. If they differ, we write the placeholder explicitly, such as `@PathVariable("userId") Long id`. It is similar to `req.params.id` in Express.

## 12. `@Builder`

`@Builder` implements the Builder Pattern. It lets us create an object step by step with readable field names instead of a long constructor argument list.

### Without Lombok

```java
public class User {
    private Long id;
    private String name;
    private String email;

    private User(UserBuilder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.email = builder.email;
    }

    public static class UserBuilder {
        private Long id;
        private String name;
        private String email;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```

### With Lombok

```java
@Getter
@Builder
public class User {
    private Long id;
    private String name;
    private String email;
}
```

Usage:

```java
User user = User.builder()
    .id(1L)
    .name("Kaif")
    .email("kaif@example.com")
    .build();
```

Lombok generates the builder class, the chain methods and the final `build()` method during compilation.

### Why use it?

```text
Long constructor = difficult to read and easy to get wrong
Builder          = named fields, readable order and optional fields
```

Advantages:

- Field names are visible while creating the object.
- Optional fields can be omitted.
- No need to remember constructor parameter order.
- Useful for DTOs, value objects and entities with many fields.
- Works well with `final` fields for immutable objects.

## 13. `@Builder` in this project

The project uses `@Builder` on models and DTOs such as `Order`, `OrderItem`, `Inventory`, `Branch`, `SubscriptionPlan`, `StoreContact` and several response DTOs.

### `StoreContact.builder()`

`StoreContact` is declared with:

```java
@Embeddable
@Builder
public class StoreContact {
    private String address;
    private String phone;
    private String email;
}
```

The store service creates it like this:

```java
StoreContact contact = StoreContact.builder()
    .address(storeDto.getContact().getAddress())
    .phone(storeDto.getContact().getPhone())
    .email(storeDto.getContact().getEmail())
    .build();
```

This is clearer than:

```java
StoreContact contact = new StoreContact(
    address,
    phone,
    email
);
```

### `Order.builder()`

The order service builds an order with only the values needed at that point:

```java
Order order = Order.builder()
    .customer(customer)
    .branch(branch)
    .cashier(cashier)
    .paymentType(orderDto.getPaymentType())
    .status(OrderStatus.PENDING)
    .build();
```

The builder is useful here because an order has relationships, status values and multiple optional fields.

### `OrderItem.builder()`

The project also creates order items with a builder:

```java
OrderItem item = OrderItem.builder()
    .product(product)
    .quantity(quantity)
    .price(product.getSellingPrice())
    .totalPrice(product.getSellingPrice() * quantity)
    .build();
```

## 14. `@Builder.Default`

If a field has a default value and the builder should preserve it, use:

```java
@Builder.Default
private boolean active = true;
```

Without `@Builder.Default`, Lombok's builder may use Java's default value (`false` for boolean, `null` for objects) when the field is not explicitly set in the builder.

The project uses `@Builder.Default` in classes such as `SubscriptionPlan` and `SystemSettings` for default field values.

## 15. Builder and JPA entities

Builders are convenient for entities, but remember:

- JPA still needs a no-args constructor.
- The generated ID is normally assigned by the database, so it is usually not set when creating a new entity.
- Relationships should be set carefully.
- Builder defaults and `@PrePersist` defaults should not conflict.

A common combination is:

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
}
```

## 16. MERN comparison

MERN commonly uses an object literal:

```javascript
const user = {
  id: 1,
  name: "Kaif",
  email: "kaif@example.com"
};
```

Spring's equivalent readable construction is:

```java
User user = User.builder()
    .id(1L)
    .name("Kaif")
    .email("kaif@example.com")
    .build();
```

## 17. Interview answer

> Lombok's `@Builder` generates the Builder Pattern. It lets me create an object using readable named methods instead of a long constructor. In this project it is used for objects such as `StoreContact`, `Order` and `OrderItem`, where many fields and relationships are set during creation. `@Builder.Default` preserves a field's default value when the builder does not explicitly set it. For JPA entities, I still keep a no-args constructor because Hibernate needs it.

## 18. `@Data`

`@Data` is Lombok's all-in-one shortcut annotation. It bundles these annotations:

```text
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
```

Example:

```java
import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
}
```

Lombok generates getters, setters, `toString()`, `equals()`, `hashCode()` and a required-arguments constructor.

Without Lombok, we would write those methods manually:

```java
public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

@Override
public String toString() {
    return "User{id=" + id + ", name='" + name + "'}";
}
```

## 19. Best use of `@Data`

`@Data` is usually convenient for:

- Request DTOs.
- Response DTOs.
- Small data objects.
- Standard Java beans.

The project uses `@Data` on several DTOs, where automatically generated accessors are useful.

## 20. JPA entity caution

Using `@Data` directly on a JPA entity can cause problems:

```text
@EqualsAndHashCode
    = may include generated IDs, relationships or collections
    = can confuse Hibernate equality and cause recursion

@ToString
    = may print lazy relationships
    = can trigger database queries or LazyInitializationException
```

For entities, prefer explicit Lombok annotations:

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // entity fields
}
```

This gives more control over equality, string output and constructors. If equality is needed, choose the fields carefully rather than automatically including every relationship.

## 21. Interview answer

> `@Data` is a Lombok shortcut that bundles getters, setters, `toString`, `equals`, `hashCode` and a required-arguments constructor. It is convenient for DTOs and simple data classes. I use it carefully with JPA entities because generated equality and `toString` methods may include IDs, lazy relationships or collections and cause Hibernate performance or recursion problems. For entities, explicit annotations give better control.
