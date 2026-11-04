# JJWT + Jackson — Easy Revision Notes

Remember this simple idea: JJWT creates and verifies JWTs, Jackson handles JSON conversion, and Spring Security manages authentication and authorization.

## 1. What are the three dependencies?

In MERN, you install one package:

```bash
npm install jsonwebtoken
```

In Spring Boot, JJWT is divided into three Maven dependencies.

| Dependency | Simple meaning | Main job |
| --- | --- | --- |
| `jjwt-api` | The interface you use | Provides `Jwts`, `Claims`, builder and parser APIs |
| `jjwt-impl` | The engine | Implements the JJWT APIs at runtime |
| `jjwt-jackson` | The JSON converter | Converts Java objects to JSON and JSON back to Java objects |

Memory trick: API = what you use, Impl = how it works, Jackson = JSON conversion.

![JJWT modular architecture: API, implementation, and Jackson responsibilities](../images/jjwt-modular-architecture.png)

### Maven configuration (`pom.xml`)

```xml
<!-- Public API: available when compiling Java code -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- Actual implementation: needed at runtime -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- JSON conversion: needed at runtime -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

- Maven is the dependency manager, like npm.
- `pom.xml` declares which libraries the project needs.
- Maven downloads them into its local repository and makes them available on the appropriate classpaths.
- `jjwt-api` is needed during compilation. The implementation and Jackson integration are needed when running the application.

## 2. What does Jackson actually do?

A JWT has three parts:

```text
Header:    algorithm and token type
Payload:   email, role, issue time and expiration
Signature: helps detect tampering
```

The JWT format is:

```text
header.payload.signature
```

Suppose your Java code contains this map:

```java
Map<String, Object> claims = new HashMap<>();
claims.put("role", "ROLE_CASHIER");
```

Jackson helps convert Java claim values into JSON, such as:

```json
{
  "role": "ROLE_CASHIER"
}
```

JJWT then handles the JWT-specific work, including Base64URL encoding, signing and parsing.

Important: Jackson does not independently generate the complete JWT, store it, or authenticate the user. It handles JSON serialization and deserialization.

## 3. How does the project generate a JWT?

The project's `JwtUtil.generateToken()` method creates the token.

```java
return Jwts.builder()
        .setClaims(claims)
        .setSubject(email)
        .setIssuedAt(new Date())
        .setExpiration(expirationDate)
        .signWith(key)
        .compact();
```

Understand each method:

| Method | Meaning |
| --- | --- |
| `Jwts.builder()` | Start building a JWT |
| `setClaims(claims)` | Add custom data, such as the role |
| `setSubject(email)` | Set the subject (`sub`), usually the user's identity |
| `setIssuedAt(...)` | Set the issue time (`iat`) |
| `setExpiration(...)` | Set the expiry time (`exp`) |
| `signWith(key)` | Sign the token using the signing key |
| `compact()` | Produce the final JWT string |

The flow is:

```text
Java claims + email + timestamps
              ↓
JJWT + Jackson JSON conversion
              ↓
Base64URL encoding + signing
              ↓
Final JWT string
```

## 4. How does the backend validate a JWT?

When a user requests a protected endpoint, the frontend sends:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The project's `JwtFilter` processes the request:

1. Read the `Authorization` header.
2. Check for the `Bearer` prefix.
3. Remove `Bearer ` using `substring(7)`.
4. Parse the JWT and verify its signature with the secret key.
5. Extract the email and check expiration.
6. Load the user and current authorities from the database.
7. Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
8. Let Spring Security check authorization rules.

### Why `substring(7)`?

```java
String token = header.substring(7);
```

`Bearer ` contains seven characters: six letters plus one space. This removes the prefix and leaves the JWT.

### What is `SecurityContextHolder`?

It holds the security context for the current request/thread, including the authenticated user.

`UsernamePasswordAuthenticationToken` is used here to represent the authenticated identity and authorities. It does not mean the password is being checked again.

## 5. Where is the token stored?

JJWT does not choose or manage token storage.

In this project:

| Stage | What happens |
| --- | --- |
| Backend | `JwtUtil` creates the token |
| Response | Token is returned in `AuthResponse` |
| Frontend | Token is stored in `localStorage` as `authToken` |
| Next request | Frontend sends `Authorization: Bearer <token>` |
| Backend | Validates the token and authenticates the request |
| Database | Issued JWTs are not individually persisted |

The backend is stateless with respect to issued JWT storage. It can still query the database to load the user's current details and authorities.

### Security note

`localStorage` is accessible to JavaScript, so an XSS vulnerability can expose the token. An appropriately configured `HttpOnly`, `Secure` cookie can reduce JavaScript-based token theft, but cookies introduce CSRF considerations and require appropriate configuration.

## 6. MERN vs Spring Boot

| Task | Node.js / MERN | Spring Boot project |
| --- | --- | --- |
| Dependency manager | npm | Maven |
| JWT library | `jsonwebtoken` | JJWT |
| Generate JWT | `jwt.sign()` | `Jwts.builder()...compact()` |
| Verify JWT | `jwt.verify()` | JJWT parser and signing key |
| JSON conversion | Handled internally by package | Jackson integration via `jjwt-jackson` |
| Request authentication | Express middleware | `JwtFilter` |
| Authorization | Middleware/application logic | Spring Security rules and `AccessControlService` |
| Current client storage | Often localStorage or cookies | `localStorage` |
| Security context | Usually application-defined | `SecurityContextHolder` |

The three JJWT dependencies collectively provide the JWT-library functionality that you commonly get from one `jsonwebtoken` package in Node.js. Spring Security is a separate framework.

## 7. Important interview follow-ups

### Q1. Is a JWT encrypted?

Usually, no. This project creates a signed JWS, not an encrypted JWT. Its header and payload are Base64URL-encoded and can be decoded. The signature detects tampering; it does not hide the contents. Never put secrets or sensitive data in ordinary JWT claims.

### Q2. Why put the role in the token if the backend loads the user again?

The role claim records the role at issuance time. This project loads current authorities from the database, allowing current database roles to drive authorization. The trade-off is an additional database lookup per authenticated request unless optimized.

### Q3. Authentication vs authorization?

- Authentication: Who are you?
- Authorization: What are you allowed to access?

JWT validation contributes to authentication. Spring Security rules and `AccessControlService` handle authorization.

### Q4. Why keep all three JJWT versions compatible?

The implementation must match the API it implements. Incompatible versions can cause runtime linkage errors or API changes. This project uses `0.11.5` consistently, while the transcript mentions `0.12.6`; code may require migration changes between these versions.

## 8. Interview answer to memorize

> JJWT is a modular Java library for working with JWTs. `jjwt-api` provides the public APIs, `jjwt-impl` supplies their runtime implementation, and `jjwt-jackson` integrates Jackson for JSON serialization and deserialization of claims. Jackson does not create or store tokens independently. In my project, `JwtUtil` generates and verifies JWTs, `JwtFilter` authenticates requests, the frontend stores the returned token in localStorage, and Spring Security manages the security context and authorization.


## 9. Quick revision quiz

1. Which dependency handles JSON conversion?
   - `jjwt-api`
   - `jjwt-impl`
   - `jjwt-jackson`

2. Which method produces the final JWT string?
   - `setClaims()`
   - `compact()`
   - `setExpiration()`

3. What does `substring(7)` remove?
   - The signature
   - The `Bearer ` prefix and space
   - The payload

4. Does a normal signed JWT hide its payload?
   - Yes, always
   - No, it is readable after decoding
   - Only when using Jackson

5. Where does this project store the token on the frontend?
   - `jjwt-jackson`
   - `SecurityContextHolder`
   - `localStorage` as `authToken`

Final memory shortcut:

```text
API = use it | Impl = run it | Jackson = JSON | JwtUtil = generate/verify
JwtFilter = authenticate | Spring Security = authorize | localStorage = store on client
```
