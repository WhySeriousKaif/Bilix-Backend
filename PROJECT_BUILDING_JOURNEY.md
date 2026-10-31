# Bilix POS — Project-Building Journey

This document records the practical problems encountered while building Bilix POS, what caused them, how they were solved, and what should be done better in a production system. It is intended as an interview-preparation timeline.

> This version is based on the project code and issues discussed so far. It is not a verbatim reconstruction of the video transcript. When the transcript is provided, add the teacher's exact issue, solution, and timestamp to the relevant section.

## 1. Authentication and protected routes

### Problem

Login and signup must work without a bearer token, while protected `/api/**` endpoints must require authentication. Testing login in Postman with **No Auth** initially caused confusion.

### Lesson

Public endpoints must be explicitly permitted; the frontend sends a bearer token only after login.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)
```

In Postman:

- Login/signup: `No Auth`.
- Protected request: `Authorization: Bearer <jwt>`.

## 2. JWT claims and reading email from a token

### Problem

The token stored the email as a custom claim, so code needed to parse the claim using the same secret key and verify the signature.

### Lesson

Never trust a decoded token without signature and expiration validation.

```java
Claims claims = Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();

String email = claims.get("email", String.class);
```

The secret must be long, private, and stored in environment configuration.

## 3. CORS and frontend/backend connection

### Problem

The frontend could show “refused to connect” or browser CORS errors even when the Spring Boot backend was running.

### Lesson

Check all three layers: backend port, frontend API base URL, and allowed browser origin.

```java
cfg.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "http://localhost:8000"
));
cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

Use the exact origin—protocol, host, and port must match.

## 4. `findById` and `Optional`

### Problem

`findById(id)` was assigned to `User`, but Spring Data returns `Optional<User>`.

```java
User user = userRepository.findById(id); // incorrect
```

### Correct solution

```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserException("User not found"));
```

`Optional` makes the “may not exist” result explicit and prevents accidental null handling.

## 5. Large error responses

### Problem

Raw Spring exceptions produced long, confusing JSON responses.

### Solution

Centralize errors with `@RestControllerAdvice`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserException.class)
    ResponseEntity<ApiError> handleUser(UserException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError("USER_NOT_FOUND", ex.getMessage()));
    }
}
```

The frontend can then consistently display a short message while logs retain debugging details.

## 6. Entity/DTO separation and mapping

### Problem

Returning JPA entities directly exposes persistence details and relationships. Mapping code also became repetitive.

### Solution

Use request/response DTOs and a mapper:

```java
public static StoreDto toDto(Store store) {
    StoreDto dto = new StoreDto();
    dto.setId(store.getId());
    dto.setBrand(store.getBrand());
    return dto;
}
```

The controller receives a request DTO, the service works with domain entities, and the response is mapped back to a DTO.

## 7. Stream mapping and naming confusion

### Problem

A variable named `dtos` actually contained `Store` entities, which made this pipeline difficult to understand:

```java
dtos.stream().map(StoreMapper::toDto).toList();
```

### Solution

Use names that describe the actual type:

```java
List<Store> stores = storeRepository.findAll();
return stores.stream()
    .map(StoreMapper::toDto)
    .toList();
```

## 8. Lifecycle timestamps

### Problem

Created/updated timestamps were easy to forget or set inconsistently.

### Solution

Use JPA lifecycle callbacks:

```java
@PrePersist
void beforeCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
}

@PreUpdate
void beforeUpdate() {
    updatedAt = LocalDateTime.now();
}
```

In production, a common auditable base class can remove duplication.

## 9. Repetitive feature structure

### Problem

User, store, product, category, employee, and customer features repeat the same layers: model, repository, service, mapper, DTO, controller.

### Lesson

The repetition is intentional layering, but shared behavior can be standardized—not blindly copied. Keep resource-specific rules in the service and use reusable validation, pagination, error, and response patterns.

## 10. UI components and visual regressions

### Problem

Prebuilt UI components and rushed simplification changed alignment, button sizing, modals, charts, and dark-theme colors.

### Solution

Build small native React components with the existing visual tokens, then verify each screen against the previous UI. Keep behavior and layout separate from data fetching.

Interview explanation:

```jsx
function ActionButton({ children, onClick, danger = false }) {
  return (
    <button className={danger ? 'button button-danger' : 'button'} onClick={onClick}>
      {children}
    </button>
  );
}
```

## 11. Dashboard data accidentally becoming static

### Problem

Dashboard cards and charts looked correct but could display hard-coded values or mock fallback data.

### Lesson

The UI should render API response state, not invented values:

```jsx
const [dashboard, setDashboard] = useState(null);

useEffect(() => {
  dashboardAPI.getSummary().then(setDashboard);
}, []);

return <span>{dashboard?.totalSales ?? 0}</span>;
```

Development mock data must never silently appear in production.

## 12. Search and filter behavior

### Problem

It was unclear whether search buttons called an API or filtered existing data.

### Lesson

Small loaded lists can filter locally; large lists should use debounced server-side search.

```http
GET /api/orders/branch/1?search=rahul&page=0&size=20
```

Always document whether a control changes local state, triggers an API call, or does both.

## 13. Pagination was missing

### Problem

Loading every order, product, or customer does not scale.

### Solution

Use backend pagination:

```http
GET /api/orders/branch/1/paged?page=0&size=20&sortBy=createdAt&direction=desc
```

The frontend stores `content`, `page`, `size`, and `totalElements`, then renders Next/Previous controls.

## 14. Customer N+1 requests

### Problem

The cashier customer page calls `GET /customers`, then calls `GET /orders/customer/{id}` once per customer to calculate order counts and total spending.

### Lesson

This is easy to understand but inefficient. Prefer one backend summary query:

```http
GET /api/customers/summary?page=0&size=20
```

The response should include `orderCount` and `totalSpent` directly.

## 15. Returns and refund safety

### Problem

The browser calculates the refund amount from selected quantities, but a malicious client could alter that amount.

### Solution

The browser sends an intent; the backend recalculates trusted prices and validates refundable quantity:

```http
POST /api/refunds
```

```json
{
  "orderId": 102,
  "reason": "Defective item",
  "amount": 749.0,
  "paymentType": "CASH"
}
```

Use a database transaction to update refund, inventory, order status, and shift totals atomically.

## 16. Shift start/end and logout

### Problem

Logging out immediately could leave an active shift open, and accidental clicks could terminate a cashier session.

### Solution

Ask for confirmation, close the shift on the backend, then clear authentication:

```http
PATCH /api/shift-reports/end?shiftReportId=12
```

Only after success:

```js
dispatch(logoutUser());
navigate('/auth/login');
```

## 17. Hard-coded IDs

### Problem

Early pages used `branchId = 1` and `cashierId = 1` as defaults.

### Risk

One cashier could see or modify another branch's data.

### Solution

Derive identity and ownership from the authenticated JWT/profile, then enforce it again in backend authorization.

## 18. What this journey teaches

The project progressed from making endpoints work to making the system reliable:

```text
CRUD endpoints
   ↓
DTOs, validation, JWT, roles
   ↓
Consistent frontend state and UI
   ↓
Pagination, tests, error handling
   ↓
CI/CD, monitoring, secure deployment
   ↓
Reliable data for ML features
```

Machine learning and DevOps should build on trustworthy transactional data; they should not hide weak authentication, inaccurate totals, or untested business logic.

## Interview answer template

For any issue, explain:

1. What the user observed.
2. What caused it technically.
3. How the fix changed the request, state, or database flow.
4. How the fix was verified.
5. What production improvement would come next.

## Transcript-derived challenges from the course journey

The supplied course transcript shows several real debugging moments. These are likely issues you could face while following the same build, and they map directly to patterns in Bilix.

### 19. Database does not exist or the JDBC URL is wrong

The teacher started the application before creating the configured MySQL database. Hibernate could not determine JDBC metadata. A second issue was using a hyphen in a database name, which produced SQL parsing problems.

Typical checks:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pos_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

Create the database separately, use an underscore instead of a hyphen, confirm MySQL is running, and verify the port, username, and password. In production, use migrations rather than relying on `ddl-auto=update`.

### 20. Spring Security generated a default password

Before the custom `UserDetailsService` was connected, Spring Security generated a temporary default password. That is a signal that the application is still using Spring's default authentication configuration.

The fix is to provide a custom user lookup and a password encoder:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Then load users by email and return their authorities. Never store or compare raw passwords.

### 21. JWT bearer prefix handling

The transcript explicitly removes the `Bearer ` prefix before parsing:

```java
if (header == null || !header.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}

String token = header.substring(7);
```

The likely failure is parsing the entire header instead of the token, or accepting malformed headers. Validate the signature, expiration, and required claims, and return a consistent 401 response.

### 22. CORS configuration can fail during startup

The transcript temporarily returned a null CORS configuration source while debugging. That can make the application start but cause browser requests to fail later.

Use a real `CorsConfigurationSource`, allow the exact frontend origin, and include `OPTIONS` for preflight requests. Test both browser and Postman because Postman does not enforce browser CORS rules.

### 23. Missing entity identifier during signup

The teacher received an “identifier of entity is null” error because the ID was not generated or copied correctly in the DTO/mapper flow.

Make the entity ID generation explicit:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Do not copy a null client-provided ID into a new entity. Map only fields the client is allowed to set.

### 24. Exception messages were too large or unclear

The transcript shows a missing-user response becoming a long internal error before the custom message was fixed.

Use a domain exception and global handler:

```java
@ExceptionHandler(UserException.class)
ResponseEntity<ApiError> handle(UserException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("USER_NOT_FOUND", ex.getMessage()));
}
```

Do not expose stack traces, SQL, or internal class names to the client.

### 25. Repository return types were misunderstood

The transcript repeatedly uses `findById`, and the same issue appeared in our work: Spring Data returns `Optional<T>`, not `T`.

```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserException("User not found"));
```

This removes ambiguous null handling and gives a predictable 404 path.

### 26. Transient order-item entity was not persisted

While creating an order, Hibernate reported that a transient `OrderItem` had to be saved before flushing. The item object existed in memory but had not been persisted or correctly cascaded.

Two valid designs are:

```java
order.addItem(item);
orderRepository.save(order); // with correct cascade settings
```

or explicitly save the item after the order exists. The relationship must have the correct owning side, `mappedBy`, and cascade configuration.

### 27. `order` is a SQL reserved word

The transcript encountered SQL syntax errors because a table was named `order`. Map it to a safe table name:

```java
@Entity
@Table(name = "orders")
public class Order { }
```

This is especially important when Hibernate generates foreign-key SQL for `order_item`.

### 28. Missing `mappedBy` in a one-to-many relationship

Order creation kept failing until the bidirectional relationship was mapped correctly. A typical mapping is:

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
           orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private Order order;
```

The child must also be attached to the parent before saving.

### 29. Nullable relationships in refunds

Refund mapping initially assumed a shift report always existed, but a refund can be created without an active shift in some flows. Accessing a null relationship caused mapping errors.

```java
dto.setShiftReportId(
    refund.getShiftReport() == null
        ? null
        : refund.getShiftReport().getId()
);
```

The database constraint and business rule must agree: either require an active shift or explicitly permit a nullable shift reference.

### 30. Shift date-range boundary mistakes

The transcript corrected an end-of-day value to `23:59:59`, which is a common reporting bug. A safer Java approach is:

```java
LocalDateTime start = date.atStartOfDay();
LocalDateTime end = date.plusDays(1).atStartOfDay();
// Query: createdAt >= start AND createdAt < end
```

Using an exclusive next-day boundary avoids missing fractional seconds at the end of the selected day.

### 31. DTO/entity field mismatch

The transcript contains several corrections where a DTO, entity, mapper, or repository used a slightly different field name or type. This can produce compile errors or silently empty responses.

Keep one clear contract and test it end to end:

```text
React request → controller DTO → service entity → repository
repository entity → mapper → response DTO → React state
```

### 32. Payment and external-service configuration

The course introduced Razorpay, Stripe, email, Cloudinary, and JWT dependencies separately. A likely failure is adding a dependency without configuring its key, webhook, environment variable, or test mode.

Keep secrets outside source code:

```properties
payment.razorpay.key-id=${RAZORPAY_KEY_ID}
payment.razorpay.secret=${RAZORPAY_SECRET}
```

Implement payment creation, verification/webhooks, and order finalization as separate steps; never mark an order paid only because the browser said so.

### 33. Deployment-only failures

The transcript demonstrated that hosted behavior can differ from local behavior. Typical causes are missing Vercel environment variables, wrong backend URL, CORS not allowing the deployed frontend, timezone differences, or a cold backend.

Verify deployed configuration with a health endpoint, inspect browser network requests, and test the exact deployed origin. Keep local and production `.env` values separate.

## How to explain these issues in an interview

Use this structure:

```text
Symptom → root cause → smallest safe fix → verification → production improvement
```

Example:

```text
Order creation failed with a transient entity error.
The OrderItem had not been persisted and the relationship lacked correct cascade/mappedBy.
I fixed the owning side and saved the aggregate consistently.
I verified POST /api/orders and then GET /api/orders/{id}.
For production, I added a transaction and integration test.
```
