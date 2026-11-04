# Molla POS — HLD Interview Flow up to User API

This note explains the project from a bird's-eye view up to authentication and the User API. It intentionally does not cover the Store API or later business modules.

## 1. What has been built so far?

```text
React frontend
    ↓ HTTP/JSON requests
Spring Boot backend
    ↓
Security + JWT authentication
    ↓
Auth and User controllers
    ↓
Services and repositories
    ↓
SQL database
```

The main pieces are:

```text
Database configuration
Entity/model classes
Repositories
Request and response DTOs
Authentication and registration APIs
JWT generation and validation
Spring Security filter chain
User APIs
Global exception handling
```

## 2. High-level architecture

```text
┌──────────────────────────────┐
│ React frontend                │
│ Login, signup, profile pages  │
└──────────────┬───────────────┘
               │ JSON + Authorization header
               ▼
┌──────────────────────────────┐
│ Spring Boot REST API          │
│ Controllers                   │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Security filter chain         │
│ RateLimitingFilter → JwtFilter│
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Services                      │
│ AuthService / UserService     │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ Repositories                  │
│ UserRepository, etc.          │
└──────────────┬───────────────┘
               ▼
┌──────────────────────────────┐
│ SQL database                  │
└──────────────────────────────┘
```

## 3. Application startup

When the application starts:

```text
1. Spring Boot starts the application context.
2. It reads application.properties.
3. It connects to the SQL database.
4. It scans controllers, services, repositories and components.
5. It creates beans such as JwtUtil, PasswordEncoder and AuthenticationManager.
6. It builds the Spring Security filter chain.
7. It starts accepting HTTP requests.
```

Important beans in this stage:

```java
@Bean
public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
}
```

```java
@Bean
public AuthenticationManager authManager(
        AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
}
```

`JwtUtil` is a Spring component. It receives the JWT secret, creates the signing key and handles token generation and parsing.

## 4. Database layer

The `User` entity represents the users table. `UserRepository` provides database operations:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    User findByEmail(String email);
}
```

The service does not write SQL directly:

```java
User user = userRepository.findByEmail(email);
```

Spring Data JPA creates the repository implementation and Hibernate generates the SQL.

## 5. Request and response objects

The frontend does not need to know the database entity structure.

```text
Request DTO  = data sent from frontend
Entity       = database object
Response DTO = safe data sent to frontend
```

For login:

```java
public class LoginRequest {
    private String email;
    private String password;
}
```

For authentication responses:

```java
public class AuthResponse {
    private String jwt;
    private String message;
    private UserDto user;
}
```

The `UserMapper` converts `User` to `UserDto`, so the password is not returned.

## 6. Public authentication APIs

The project exposes:

```text
POST /auth/signup
POST /auth/login
```

Security configuration marks `/auth/**` as public because a user cannot send a JWT before signup or login.

```java
.requestMatchers("/auth/**").permitAll()
```

## 7. Signup request flow

```text
React signup form
        ↓
POST /auth/signup
        ↓
AuthController.register()
        ↓
Validate RegisterRequest
        ↓
Check whether email already exists
        ↓
Check allowed self-registration role
        ↓
BCrypt-hash password
        ↓
Create User entity
        ↓
Save User through UserRepository
        ↓
JwtUtil.generateToken(email, role)
        ↓
Map User → UserDto
        ↓
Return AuthResponse with JWT and user
```

Important code from `AuthController`:

```java
User existingUser = userRepository.findByEmail(request.getEmail());

if (existingUser != null) {
    throw new BadRequestException(
        "User already exists with email: " + request.getEmail()
    );
}
```

```java
newUser.setPassword(
    passwordEncoder.encode(request.getPassword())
);

newUser = userRepository.save(newUser);

String token = jwtUtil.generateToken(
    newUser.getEmail(),
    newUser.getRole().toString()
);
```

The response is:

```text
HTTP 201 Created
{
  "jwt": "...",
  "message": "User registered successfully",
  "user": { ...safe user fields... }
}
```

## 8. Login request flow

```text
React login form
        ↓
POST /auth/login
        ↓
AuthController.login()
        ↓
Validate LoginRequest
        ↓
AuthenticationManager.authenticate(email, password)
        ↓
CustomUserImplementation loads user by email
        ↓
BCrypt compares raw password with stored hash
        ↓
Authentication succeeds or fails
        ↓
Load User from UserRepository
        ↓
Update lastLoginAt
        ↓
JwtUtil.generateToken(email, role)
        ↓
Map User → UserDto
        ↓
Return AuthResponse
```

The important authentication call is:

```java
authManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.getEmail(),
        request.getPassword()
    )
);
```

If the credentials are wrong, the controller throws:

```java
throw new BadRequestException("Invalid email or password");
```

Successful response:

```text
HTTP 200 OK
{
  "jwt": "...",
  "message": "Login successfully",
  "user": { ...safe user fields... }
}
```

## 9. How the JWT is created

`JwtUtil.generateToken()` stores the email as the standard subject and the role as a custom claim:

```java
Map<String, Object> claims = new HashMap<>();
claims.put("role", role);

return Jwts.builder()
    .setClaims(claims)
    .setSubject(email)
    .setIssuedAt(new Date())
    .setExpiration(expiry)
    .signWith(key)
    .compact();
```

Conceptually, the token contains:

```json
{
  "sub": "kaif@example.com",
  "role": "ROLE_STORE_ADMIN",
  "iat": "issued time",
  "exp": "expiry time"
}
```

The JWT is signed, not encrypted. The signature detects tampering; it does not hide the claims.

## 10. Frontend token storage and later requests

After login, the frontend stores the token:

```javascript
localStorage.setItem('authToken', response.jwt);
```

The common API helper adds it to later requests:

```javascript
const token = localStorage.getItem('authToken');

if (token) {
  headers['Authorization'] = `Bearer ${token}`;
}
```

The actual HTTP header is:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 11. Security filter flow for a protected request

Suppose the frontend calls:

```http
GET /api/users/profile
Authorization: Bearer <jwt>
```

The request passes through:

```text
Request
  ↓
RateLimitingFilter
  ↓
JwtFilter
  ↓
UsernamePasswordAuthenticationFilter position
  ↓
Authorization rules
  ↓
UserController
```

The project registers:

```java
http.addFilterBefore(
    filter,
    UsernamePasswordAuthenticationFilter.class
);

http.addFilterBefore(
    new RateLimitingFilter(),
    JwtFilter.class
);
```

## 12. What `JwtFilter` does

```text
1. Read Authorization header.
2. Check for Bearer prefix.
3. Remove Bearer and the space.
4. Parse and verify JWT signature and expiry.
5. Extract email from the subject.
6. Load current user details from database.
7. Create UsernamePasswordAuthenticationToken.
8. Store authentication in SecurityContextHolder.
9. Continue the filter chain.
```

Core idea:

```java
String email = jwtUtil.extractEmail(token);
UserDetails userDetails =
    userService.loadUserByUsername(email);

UsernamePasswordAuthenticationToken auth =
    new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

SecurityContextHolder.getContext()
    .setAuthentication(auth);
```

MERN equivalent:

```javascript
const decoded = jwt.verify(token, process.env.JWT_SECRET);
req.user = await User.findOne({ email: decoded.sub });
next();
```

## 13. Stateless security

The project uses:

```java
.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
```

This means the server does not keep a login session between requests.

```text
Each protected request must carry its JWT.
JwtFilter validates it again.
SecurityContextHolder exists for the current request only.
```

## 14. User APIs built so far

The User controller is mapped to:

```text
/api/users
```

### Get current profile

```http
GET /api/users/profile
Authorization: Bearer <jwt>
```

Controller flow:

```java
User user = userService.getUserFromJwt(jwt);
return ResponseEntity.ok(UserMapper.toDto(user));
```

The service extracts the email from the JWT, searches the repository and returns the complete current user:

```java
String email = jwtUtil.extractEmail(cleanToken);
User user = userRepository.findByEmail(email);

if (user == null) {
    throw new NotFoundException("User not found");
}

return user;
```

### Get user by ID

```http
GET /api/users/{id}
Authorization: Bearer <jwt>
```

```java
User user = userService.getUserById(id);
return ResponseEntity.ok(UserMapper.toDto(user));
```

Repository lookup:

```java
return userRepository.findById(id)
    .orElseThrow(() ->
        new NotFoundException("User not found with id: " + id)
    );
```

### Get all users

```http
GET /api/users/all
Authorization: Bearer <jwt>
```

The controller loads users, maps every entity to `UserDto` and returns a list.

## 15. `getCurrentUser()` alternative

The service also has a method that uses the authentication already created by `JwtFilter`:

```java
String email = SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getName();

User user = userRepository.findByEmail(email);
```

The chain is:

```text
UserDetails username = email
        ↓
Authentication principal = UserDetails
        ↓
SecurityContextHolder stores Authentication
        ↓
authentication.getName() returns email
        ↓
UserRepository.findByEmail(email)
```

This is similar to `req.user.email` in Express.

## 16. Authorization rules

Security decides whether a request is public or protected:

```java
.requestMatchers("/auth/**").permitAll()
.requestMatchers("/api/super-admin/**")
    .hasRole("ADMIN")
.anyRequest().authenticated()
```

```text
permitAll()      = no JWT required
authenticated()  = valid JWT required
hasRole("ADMIN") = valid JWT plus ADMIN authority required
```

Authentication answers “Who are you?” Authorization answers “Are you allowed?”

## 17. Error flow

```text
Service detects problem
        ↓
throw NotFoundException / BadRequestException
        ↓
GlobalExceptionHandler
        ↓
Consistent JSON error response
```

Example:

```json
{
  "timestamp": "2026-10-26T12:00:00Z",
  "error": "NOT_FOUND",
  "message": "User not found",
  "status": 404
}
```

## 18. Full protected profile request

```text
1. User logs in and receives JWT.
2. React stores JWT in localStorage.
3. React sends GET /api/users/profile.
4. api.js adds Authorization: Bearer <jwt>.
5. CORS allows the frontend origin and Authorization header.
6. RateLimitingFilter checks request frequency.
7. JwtFilter extracts and verifies the token.
8. JwtFilter loads current authorities.
9. JwtFilter stores Authentication in SecurityContextHolder.
10. SecurityConfig checks authenticated access.
11. UserController receives the header.
12. UserService extracts email and loads User.
13. UserMapper removes internal fields and creates UserDto.
14. Spring returns JSON to React.
```

## 19. Interview explanation

> This application is a stateless JWT-based Spring Boot REST API with a React frontend. During signup, the backend validates the request, hashes the password with BCrypt, saves the user through Spring Data JPA and returns a signed JWT. During login, `AuthenticationManager` verifies the email and password using `UserDetailsService` and the password encoder. `JwtUtil` then creates a token containing the email and role. The frontend stores the token and sends it in the `Authorization: Bearer` header for protected requests. `JwtFilter` runs before the controller, verifies the token, loads current user authorities and stores an authentication object in `SecurityContextHolder`. Security configuration then applies authorization rules. The User controller loads the current user, maps the entity to a safe `UserDto` and returns the response. Exceptions are converted into consistent JSON by `GlobalExceptionHandler`.

## 20. Final memory diagram

```text
SIGNUP / LOGIN
Frontend
  → AuthController
  → PasswordEncoder / AuthenticationManager
  → UserRepository
  → JwtUtil
  → AuthResponse(JWT + UserDto)

PROTECTED USER REQUEST
Frontend + Bearer JWT
  → CORS
  → RateLimitingFilter
  → JwtFilter
  → JwtUtil + UserDetailsService
  → SecurityContextHolder
  → SecurityConfig authorization
  → UserController
  → UserService
  → UserRepository
  → UserMapper
  → UserDto JSON response
```
