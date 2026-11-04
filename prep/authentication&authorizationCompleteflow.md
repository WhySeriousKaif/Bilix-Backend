Perfect. We'll follow your 1–6 learning roadmap in exactly that order. We'll study the actual code from your POS backend project, connect it with the JWT concepts we've already covered, and understand what happens behind the scenes.

We'll start with:

# Part 1 — Complete Authentication Flow

Our first goal is to understand how a new user registers and how an existing user logs in.

I've got your `AuthController`, DTOs, `AuthServiceImp`, `CustomUserImplementation`, `UserRepository`, `User`, `UserRole`, `JwtUtil`, and `AuthResponse` code.

> [!IMPORTANT]
> **Important architectural distinction before we begin:**
> Your project contains two different authentication implementations:
> - `AuthController` uses `AuthenticationManager` and `JwtUtil`.
> - The earlier `AuthServiceImp` uses its own manual `authenticate()` method and also uses `JwtUtil`.
>
> These are two different approaches to authentication. They shouldn't be confused as one continuous flow. We'll follow the current `AuthController` implementation first, because that's the controller you've provided. I'll also explain how the earlier service implementation differs.


---

# 1. First understand the complete picture

Imagine you're building a POS (Point of Sale) application.

A store owner wants to register. Later, the store owner wants to log in to manage the store.

There are two operations:

Signup:

- User submits registration details.
- Backend validates the details.
- Backend checks whether the user already exists.
- Password is hashed.
- User is saved in MySQL.
- JWT is generated.
- User receives the JWT and profile information.

Login:

- User submits email and password.
- Spring Security checks the credentials.
- User is retrieved from the database.
- JWT is generated.
- User receives the JWT and profile information.

Here's the architecture:

## Authentication architecture

React frontend / Postman

Signup or Login request

AuthController

Receives HTTP request

Signup path

RegisterRequest → Validation → PasswordEncoder

UserRepository → MySQL

Login path

LoginRequest → AuthenticationManager

CustomUserImplementation → UserRepository

PasswordEncoder verifies password

JwtUtil

Generates signed JWT

AuthResponse

JWT + message + user details

Let's start from the first files.


---

# 2. LoginRequest.java and RegisterRequest.java

These are called DTOs (Data Transfer Objects).

First, what is a DTO?

A DTO is an object used to transfer data between different parts of an application, especially between the frontend and backend.

Imagine your React frontend sends this JSON to your backend:

```json
{
  "email": "kaif@gmail.com",
  "password": "password123"
}
```

Spring Boot needs a Java object to receive this data.

That's why we create `LoginRequest`.

## A. LoginRequest.java

Your code:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

Let's understand the annotations.

### 1. @Data

This annotation comes from Lombok.

Normally, if you create a Java class, you need to write:

```java
public String getEmail() {
    return email;
}

public void setEmail(String email) {
    this.email = email;
}
```

You also need to write `toString()`, `equals()` and `hashCode()` if required.

Instead, Lombok's:

```java
@Data
```

generates getters, setters, `toString()`, `equals()` and `hashCode()` at compile time.

So you don't have to write all those methods manually.

### 2. @AllArgsConstructor

```java
@AllArgsConstructor
```

Generates a constructor with all class fields as parameters.

Equivalent to:

```java
public LoginRequest(String email, String password) {
    this.email = email;
    this.password = password;
}
```

### 3. @NoArgsConstructor

```java
@NoArgsConstructor
```

Generates an empty constructor.

Equivalent to:

```java
public LoginRequest() {
}
```

Why do we need this?

Spring/Jackson commonly uses a no-argument constructor and setters to create and populate DTO objects when converting incoming JSON into Java objects.

### 4. @Builder

```java
@Builder
```

Allows us to create objects using the builder pattern.

For example:

```java
LoginRequest request = LoginRequest.builder()
        .email("kaif@gmail.com")
        .password("password123")
        .build();
```

Instead of passing parameters into a constructor, we specify the fields by name.

### 5. @Getter and @Setter

These generate getters and setters.

But notice something!

You've already used:

```java
@Data
```

`@Data` already generates getters and setters.

Therefore, `@Getter` and `@Setter` are redundant here.

You can simply use:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {
    // fields
}
```

### 6. @NotBlank

```java
@NotBlank(message = "Email is required")
private String email;
```

This is a validation annotation.

It checks that the string:

- Is not null.
- Is not empty.
- Does not contain only whitespace.

For example:

| Input              | Result  |
| ------------------ | ------- |
| `null`             | Invalid |
| `""`               | Invalid |
| `"   "`            | Invalid |
| `"kaif@gmail.com"` | Valid   |

### 7. @Email

```java
@Email(message = "Email should be valid")
```

Checks whether the supplied value has a valid email-like format.

For example:

```text
kaif@gmail.com  → Valid format
kaifgmail.com   → Invalid format
```

Note that this doesn't verify whether the email actually exists or belongs to the user.

### 8. Password validation

```java
@NotBlank(message = "Password is required")
private String password;
```

This ensures that the password isn't empty or blank.

It does not check the password's correctness. That happens during login.

## B. RegisterRequest.java

Now let's understand registration.

Your code:

```java
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    private Long branchId;
    private Long storeId;
}
```

Let's understand the additional annotations and fields.

### 1. @Size

```java
@Size(min = 6)
private String password;
```

Ensures the password contains at least six characters.

For example:

```text
12345       → Invalid
123456      → Valid
password123 → Valid
```

### 2. @NotNull

```java
@NotNull(message = "Role is required")
private UserRole role;
```

This ensures that a role is provided.

Why do we use `@NotNull` instead of `@NotBlank`?

Because `role` is not a String.

It's an enum:

```java
public enum UserRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CASHIER,
    ROLE_BRANCH_MANAGER,
    ROLE_STORE_MANAGER,
    ROLE_STORE_ADMIN,
    ROLE_STORE_EMPLOYEE,
    ROLE_BRANCH_CASHIER
}
```

`@NotBlank` is for character sequences. `@NotNull` checks whether an object reference is null.

### 3. What are branchId and storeId?

```java
private Long branchId;
private Long storeId;
```

These are optional fields used to associate a user with a particular branch or store.

For example:

Imagine a POS system has:

```text
Store: ABC Supermarket
    |
    |--- Branch 1: Hyderabad
    |
    |--- Branch 2: Bengaluru
```

A branch manager may need to be associated with a particular branch.

These IDs allow the backend to identify the relevant database records.

> [!WARNING]
> **Important:** Your current `AuthController` only allows `ROLE_STORE_ADMIN` to self-register. Therefore, the branch-manager and store-manager assignment code later in the controller cannot currently be reached through public signup.

### What happens when @Valid is used?

This is very important.

Your controller has:

```java
public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request
)
```

Let's understand the annotations.

- `@RequestBody`: Reads the JSON request body.
- `@Valid`: Triggers validation on the resulting Java object.
- `LoginRequest`: The object receiving the incoming data.

Behind the scenes:

Incoming frontend JSON

```json
{
  "email": "",
  "password": "123"
}
```

Jackson converts JSON into LoginRequest

Jakarta Validation checks annotations

Validation fails

Spring raises a MethodArgumentNotValidException. Your global exception handler can convert it into an HTTP error response.

Notice: If validation fails, your controller method does not proceed normally.

> [!TIP]
> **Interview Question:** What is the difference between `@RequestBody` and `@Valid`?
>
> **Answer:**
> - `@RequestBody` converts incoming JSON into a Java object.
> - `@Valid` validates that Java object using its validation annotations.


---

# 3. AuthController.java

*Entry point of authentication APIs*

Now we reach the controller.

First, understand what a Controller does.

A controller is responsible for receiving HTTP requests from the frontend and returning HTTP responses.

For example:

```text
React frontend
     |
     | POST /auth/login
     |
     v
AuthController
```

The controller receives the request and decides which application operation should execute.

## A. Understanding the class annotations

Your code:

```java
@Tag(
    name = "Authentication APIs",
    description = "User registration and login endpoints"
)
@RestController
@RequestMapping("/auth")
public class AuthController {
```

1\. @Tag

This is from Swagger/OpenAPI.

It groups your authentication APIs under a section called Authentication APIs in Swagger documentation.

2\. @RestController

This tells Spring that this class handles REST requests.

It is effectively a combination of:

```java
@Controller
@ResponseBody
```

The `@ResponseBody` behavior means returned objects are converted into HTTP response bodies, usually JSON through Jackson.

For example, returning:

```java
return authResponse;
```

can produce JSON:

```json
{
  "jwt": "eyJhbGciOi...",
  "message": "Login successfully",
  "user": {
    "email": "kaif@gmail.com"
  }
}
```

3\. @RequestMapping("/auth")

This defines the common URL prefix for all endpoints inside this controller.

For example:

```java
@PostMapping("/login")
```

becomes:

```http
POST /auth/login
```

And:

```java
@PostMapping("/signup")
```

becomes:

```http
POST /auth/signup
```

## B. Understanding dependency injection in the controller

Your code:

```java
private final AuthenticationManager authManager;
private final JwtUtil jwtUtil;
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final BranchRepository branchRepository;
private final StoreRepository storeRepository;
```

What are these variables?

They are dependencies required by your controller.

Let's understand their responsibilities.

| Dependency            | Responsibility                  |
| --------------------- | ------------------------------- |
| AuthenticationManager | Authenticates login credentials |
| JwtUtil               | Generates JWT                   |
| UserRepository        | Communicates with user database |
| PasswordEncoder       | Hashes passwords                |
| BranchRepository      | Retrieves and saves branches    |
| StoreRepository       | Retrieves stores                |

### What is dependency injection?

Imagine you want to use a UserRepository.

Without dependency injection, you might try:

```java
UserRepository repository = new UserRepository();
```

But this won't work because `UserRepository` is an interface, and Spring Data JPA provides its implementation at runtime.

Instead, Spring creates the required repository bean and injects it into your controller.

Your constructor:

```java
public AuthController(
        AuthenticationManager authManager,
        JwtUtil jwtUtil,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        BranchRepository branchRepository,
        StoreRepository storeRepository
) {
    this.authManager = authManager;
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.branchRepository = branchRepository;
    this.storeRepository = storeRepository;
}
```

Spring automatically calls this constructor and supplies the required beans.

This is called constructor-based dependency injection.

Notice that you don't have `@Autowired` on the constructor.

That's okay. When a class has one constructor, Spring automatically uses it for dependency injection.

## C. What is AuthenticationManager?

We'll discuss this more deeply during login, but first understand its responsibility.

```java
private final AuthenticationManager authManager;
```

`AuthenticationManager` is a Spring Security interface.

Its job is to authenticate the user.

For example, it receives:

```text
Email: kaif@gmail.com
Password: password123
```

It coordinates the authentication process:

1. Find the user.
2. Retrieve the stored password hash.
3. Compare the submitted password with the stored hash.
4. Check authentication success or failure.

We'll see exactly how it uses `CustomUserImplementation`.

## D. What is @Autowired(required = false)?

Your code:

```java
@Autowired(required = false)
private MailService mailService;
```

Normally, Spring expects an injected dependency to be available.

But here:

```java
required = false
```

means Spring does not have to inject this dependency.

Why?

Because your email functionality is optional.

If the application has a MailService bean, Spring injects it.

If no MailService bean exists, the field remains `null`.

Later, your code checks:

```java
if (mailService != null) {
    // Send email
}
```

This prevents the application from failing simply because email functionality isn't configured.


---

# 4. Understanding the SIGNUP flow

Now let's follow the first complete operation.

POST /auth/signup

Imagine a store owner enters the following details in your frontend:

```json
{
  "fullName": "Kaif Molla",
  "email": "kaif@gmail.com",
  "phone": "9876543210",
  "password": "password123",
  "role": "ROLE_STORE_ADMIN"
}
```

The frontend sends this request:

```http
POST /auth/signup
Content-Type: application/json
```

Let's follow the execution.

## Step 1. Receive and validate the request

Your code:

```java
@PostMapping("/signup")
public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request
) {
```

What happens?

1. Spring receives `POST /auth/signup`.
2. It identifies the matching controller method.
3. Jackson converts the JSON into a `RegisterRequest` object.
4. `@Valid` triggers the validation annotations.
5. If validation succeeds, the method executes.

Now you can access the submitted values using:

```java
request.getEmail();
request.getPassword();
request.getRole();
```

For example:

```java
request.getEmail();
```

returns:

```text
kaif@gmail.com
```

Remember, `request` is just a Java object containing the data received from the frontend.

## Step 2. Check whether the user already exists

Your code:

```java
User existingUser =
        userRepository.findByEmail(request.getEmail());

if (existingUser != null) {
    throw new BadRequestException(
        "User already exists with email: " + request.getEmail()
    );
}
```

Let's understand this.

First:

```java
userRepository.findByEmail(request.getEmail());
```

This calls your repository method:

```java
User findByEmail(String email);
```

Spring Data JPA understands the method name and generates the required database query.

Conceptually, it executes something like:

```sql
SELECT *
FROM users
WHERE email = 'kaif@gmail.com';
```

Two possibilities:

Case 1: User exists

The database returns a User entity.

```java
existingUser != null
```

The condition is true, so we throw an exception.

Case 2: User doesn't exist

The repository returns `null`.

The condition is false, and registration continues.

Why do we check this?

Because your email column is also marked as unique:

```java
@Column(nullable = false, unique = true)
private String email;
```

The application-level check provides a friendly error, while the database constraint protects against duplicate emails even if two requests arrive concurrently.

## Step 3. Check allowed roles

Your code:

```java
private static final Set<UserRole> SELF_REGISTERABLE_ROLES =
        java.util.Set.of(UserRole.ROLE_STORE_ADMIN);
```

This defines the roles allowed during public registration.

Later:

```java
if (!SELF_REGISTERABLE_ROLES.contains(request.getRole())) {
    throw new BadRequestException(
        "Cannot self-register with role: " + request.getRole()
    );
}
```

Let's understand the logic.

`contains()` checks whether the requested role exists in the set.

Suppose:

```text
Requested role = ROLE_STORE_ADMIN
```

The set contains that role, so registration continues.

But suppose:

```text
Requested role = ROLE_ADMIN
```

The set does not contain it.

The `!` operator reverses the result, so the condition becomes true and the request is rejected.

Why is this important?

Imagine if anyone could register themselves as a super admin.

That would be a serious security vulnerability.

You are implementing role-based registration to prevent unauthorized privilege escalation.

One thing to notice in your actual code: the comment describes this as allowing low-privilege roles, but the only permitted role is `ROLE_STORE_ADMIN`. The comment and implementation don't match.

Also, the later branch-manager and store-manager assignment blocks are currently unreachable through this public signup method because those roles aren't in the allowed set.

## Step 4. Create the User entity

Your code:

```java
User newUser = new User();

newUser.setEmail(request.getEmail());

newUser.setPassword(
    passwordEncoder.encode(request.getPassword())
);

newUser.setRole(request.getRole());

newUser.setFullName(request.getFullName());

newUser.setPhone(request.getPhone());

newUser.setLastLoginAt(LocalDateTime.now());

newUser.setCreatedAt(LocalDateTime.now());

newUser.setUpdatedAt(LocalDateTime.now());
```

First, what is the difference between `RegisterRequest` and `User`?

This is extremely important.

| RegisterRequest            | User                                            |
| -------------------------- | ----------------------------------------------- |
| DTO                        | Entity                                          |
| Receives frontend data     | Represents database record                      |
| Contains input fields      | Contains persistent user data and relationships |
| Used for API communication | Used by JPA/Hibernate                           |

Imagine the frontend sends:

```json
{
  "fullName": "Kaif Molla",
  "email": "kaif@gmail.com",
  "password": "password123",
  "role": "ROLE_STORE_ADMIN"
}
```

Spring creates a `RegisterRequest`.

Then you create a separate `User` entity:

```java
User newUser = new User();
```

Why?

Because we don't directly save the incoming DTO into the database.

Instead, we transfer the required values from the DTO into the entity.

For example:

```java
newUser.setEmail(request.getEmail());
```

Means:

Take the email received from the frontend and assign it to the User entity.

### The most important line: PasswordEncoder

```java
newUser.setPassword(
    passwordEncoder.encode(request.getPassword())
);
```

Let's understand what happens.

Frontend sends:

```text
password123
```

We don't want to save this plain-text password in our database.

Instead:

```java
passwordEncoder.encode("password123");
```

Using BCrypt, it generates a password hash.

Conceptually:

```text
password123
      |
      v
PasswordEncoder
      |
      v
BCrypt hashing
      |
      v
$2a$10$... (hashed password)
      |
      v
Database
```

The actual hash is not the same every time because BCrypt uses a random salt.

This is why we later use:

```java
passwordEncoder.matches(rawPassword, encodedPassword);
```

We don't decrypt the hash. We verify the submitted password against the stored hash.

> [!TIP]
> **Interview Question:** Why do we use BCrypt instead of storing plain passwords?
>
> **Answer:** BCrypt securely hashes passwords using a salt and an adjustable computational cost, making password cracking more difficult. Passwords should never be stored in plaintext.

### What is LocalDateTime.now()?

```java
newUser.setCreatedAt(LocalDateTime.now());
```

Stores the current date and time.

Similarly:

```java
newUser.setUpdatedAt(LocalDateTime.now());
newUser.setLastLoginAt(LocalDateTime.now());
```

sets the corresponding timestamps.

One design consideration: setting `lastLoginAt` during registration means it represents the registration time initially, even though the user hasn't necessarily logged in yet.

## Step 5. Branch and Store assignment

Your code:

```java
Branch branchToUpdate = null;
```

This declares a variable that will temporarily hold a Branch entity if one needs to be updated later.

You then check the requested role:

```java
if (request.getRole().equals(UserRole.ROLE_BRANCH_MANAGER)) {
    // branch assignment
}
```

If the role is `ROLE_BRANCH_MANAGER`, your code retrieves the branch:

```java
Branch branch = branchRepository.findById(
    request.getBranchId()
).orElseThrow(
    () -> new BadRequestException("Branch not found")
);
```

Let's break it down.

- `findById()` searches for the branch using its primary key.
- It returns an `Optional<Branch>`.
- `orElseThrow()` returns the Branch if present.
- If absent, it throws an exception.

Then:

```java
newUser.setBranch(branch);
newUser.setStore(branch.getStore());
```

You associate the new user with the branch and the store belonging to that branch.

Remember your User entity:

```java
@ManyToOne
private Store store;

@ManyToOne
private Branch branch;
```

These relationships allow multiple users to be associated with the same store or branch.

But remember: As explained earlier, this section cannot currently be reached through public signup because only `ROLE_STORE_ADMIN` is permitted.

## Step 6. Save user to the database

Your code:

```java
newUser = userRepository.save(newUser);
```

This is where the actual database persistence happens.

What is `save()`?

It is a method inherited from:

```java
JpaRepository<User, Long>
```

Spring Data JPA provides its implementation.

Behind the scenes, Hibernate manages the persistence operation.

For a new entity, conceptually:

```sql
INSERT INTO users (
    full_name,
    email,
    password,
    role,
    phone,
    created_at,
    updated_at,
    last_login_at
)
VALUES (...);
```

The database creates the user record.

Because your entity uses:

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

The database generates the ID.

For example:

```text
id = 101
```

The returned `User` entity can now contain that generated ID.

Why do we write:

```java
newUser = userRepository.save(newUser);
```

Instead of just:

```java
userRepository.save(newUser);
```

Because we want to use the returned entity, which may contain the generated ID and other persistence-generated values.

## Step 7. Send welcome email

Your code:

```java
if (mailService != null) {
    try {
        mailService.sendSimpleMail(
            newUser.getEmail(),
            "Welcome to Bilix POS",
            "Hi " + newUser.getFullName()
        );
    } catch (Exception e) {
        System.out.println(
            "Failed to send email: " + e.getMessage()
        );
    }
}
```

This sends a welcome email if the MailService exists.

Notice the `try-catch`.

Why?

Suppose the user is registered successfully, but the email server is down.

You don't want the user registration to fail simply because the welcome email couldn't be sent.

Your code catches the exception and continues.

This is called best-effort email delivery.

## Step 8. Generate JWT

Your code:

```java
String token = jwtUtil.generateToken(
    newUser.getEmail(),
    newUser.getRole().toString()
);
```

Now connect this with our previous JWT lesson.

Remember `JwtUtil`?

It has:

```java
public String generateToken(String email, String role)
```

It creates a token containing:

```json
{
  "sub": "kaif@gmail.com",
  "role": "ROLE_STORE_ADMIN",
  "iat": "...",
  "exp": "..."
}
```

It then signs the token using the secret key.

The final result is a JWT string.

> [!NOTE]
> **Important difference from our earlier `JwtProvider` example:**
> - Earlier, we stored email using `.claim("email", ...)`.
> - In this project, we store email using `.setSubject(email)`.
>
> Both are ways of storing identity information, but the corresponding extraction methods are different.

Here, you use:

```java
extractEmail(token)
```

which calls:

```java
getSubject()
```

## Step 9. Build the response

Your code:

```java
AuthResponse authResponse = new AuthResponse();

authResponse.setJwt(token);

authResponse.setMessage("User registered successfully");

authResponse.setUser(UserMapper.toDto(newUser));
```

Remember your `AuthResponse`:

```java
@Data
@Component
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;
}
```

It is the response object that goes back to the frontend.

Let's understand the fields.

jwt

Contains the generated authentication token.

message

Contains a success message.

user

Contains the user profile information.

Now what is:

```java
UserMapper.toDto(newUser)
```

It converts the database entity into a DTO.

Why?

Because we generally don't want to expose the entire database entity directly to the frontend.

For example, the User entity contains:

```java
private String password;
```

But the frontend should never receive the stored password hash.

A mapper converts the entity into a safe representation.

We'll inspect `UserMapper` and `UserDto` when we study the complete response flow.

## Step 10. Return HTTP response

Your code:

```java
return new ResponseEntity<>(
    authResponse,
    HttpStatus.CREATED
);
```

What is ResponseEntity?

It allows you to control the HTTP response, including:

- Response body
- HTTP status code
- HTTP headers

Here you're returning:

```http
HTTP 201 Created
```

Why 201?

Because a new user has been successfully created.

The frontend receives something similar to:

```json
{
  "jwt": "eyJhbGciOi...",
  "message": "User registered successfully",
  "user": {
    "id": 101,
    "fullName": "Kaif Molla",
    "email": "kaif@gmail.com",
    "role": "ROLE_STORE_ADMIN"
  }
}
```

The exact user fields depend on your `UserDto`.

Signup flow completed


---

# 5. Understanding the LOGIN flow

POST /auth/login

This is probably the most important part of authentication because it connects Spring Security with your database.

Imagine the user already has an account:

```text
Email: kaif@gmail.com
Password: password123
```

The frontend sends:

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "kaif@gmail.com",
  "password": "password123"
}
```

Let's follow the code.

## Step 1. Receive the login request

Your code:

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request
) {
```

This works exactly like signup.

- `@PostMapping("/login")` maps the endpoint.
- `@RequestBody` converts JSON into LoginRequest.
- `@Valid` checks the email and password validation rules.

After successful validation, we can access:

```java
request.getEmail();
request.getPassword();
```

## Step 2. AuthenticationManager authenticates the user

Your code:

```java
try {
    authManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );
} catch (Exception e) {
    throw new BadRequestException(
        "Invalid email or password"
    );
}
```

Most important login concept

Let's understand this carefully.

First:

```java
new UsernamePasswordAuthenticationToken(
    request.getEmail(),
    request.getPassword()
)
```

Remember this class from our previous JWT discussion?

It represents authentication information.

But there's something important here.

Spring Security provides two commonly used constructors:

First constructor:

```java
new UsernamePasswordAuthenticationToken(
    email,
    password
);
```

This creates an unauthenticated authentication request.

It means:

"I want Spring Security to authenticate this user."

Second constructor:

```java
new UsernamePasswordAuthenticationToken(
    email,
    password,
    authorities
);
```

This creates an authenticated token, generally used after credentials have been verified by a trusted authentication process.

We discussed the second constructor earlier in your JWT implementation.

Now let's understand what happens when you call:

```java
authManager.authenticate(authenticationToken);
```

### Behind the scenes

The flow is approximately:

AuthController

authManager.authenticate()

AuthenticationManager

Delegates authentication

DaoAuthenticationProvider

Performs username/password authentication

CustomUserImplementation

Loads user from database

UserRepository

Finds user by email

PasswordEncoder

Checks submitted password against stored hash

Authentication successful

Let's understand these components one by one.

### What is AuthenticationManager?

It is the main interface responsible for authentication.

You send it an Authentication object containing the submitted email and password.

It delegates the actual authentication to an appropriate AuthenticationProvider.

### What is DaoAuthenticationProvider?

This is a built-in Spring Security authentication provider.

It uses:

- `UserDetailsService` to load the user.
- `PasswordEncoder` to verify the password.

### How does CustomUserImplementation come into the picture?

You implemented:

```java
public class CustomUserImplementation
        implements UserDetailsService {
```

Spring Security uses this service to retrieve user information from your database.

We'll examine its code shortly.

### How is the password checked?

Suppose the database contains:

```java
Email: kaif@gmail.com

Password hash:
$2a$10$abc...
```

The frontend sends:

```text
password123
```

Spring Security calls the equivalent of:

```java
passwordEncoder.matches(
    "password123",
    storedPasswordHash
);
```

If the password matches, authentication succeeds.

If it doesn't match, Spring throws an authentication exception.

Your controller catches the exception:

```java
catch (Exception e) {
    throw new BadRequestException(
        "Invalid email or password"
    );
}
```

This gives the frontend a consistent error message.

> [!NOTE]
> **Important Distinction:** Your `AuthController` doesn't manually compare the password. It delegates that responsibility to Spring Security.


---

# 6. CustomUserImplementation.java

*Spring Security + Database integration*

Now let's understand the class responsible for loading users.

Your code:

```java
@Service
public class CustomUserImplementation
        implements UserDetailsService {
```

### A. What is UserDetailsService?

`UserDetailsService` is a Spring Security interface.

It provides a method:

```java
UserDetails loadUserByUsername(String username);
```

Its responsibility is to load user information from the database.

In your application, the username is actually the user's email.

Why do we need this?

Spring Security doesn't automatically know how your User entity is structured.

For example, your database entity has:

```java
private String email;
private String password;
private UserRole role;
```

But Spring Security expects user information through its own `UserDetails` abstraction.

So we implement `UserDetailsService` to connect our application's User entity with Spring Security.

### B. Inject UserRepository

Your code:

```java
@Autowired
private UserRepository userRepository;
```

We need the repository because we want to retrieve the user from MySQL.

### C. loadUserByUsername()

Your code:

```java
@Override
public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {
```

Remember the AuthenticationManager?

When you call:

```java
authManager.authenticate(...)
```

Spring's DaoAuthenticationProvider needs to retrieve the user.

It calls this method.

Suppose:

```text
username = kaif@gmail.com
```

### Step 1. Find the user

```java
User user = userRepository.findByEmail(username);
```

This calls:

```java
User findByEmail(String email);
```

from your UserRepository.

Conceptually, Hibernate executes:

```sql
SELECT *
FROM users
WHERE email = 'kaif@gmail.com';
```

If the user exists, we receive a User entity.

If not:

```java
if (user == null) {
    throw new UsernameNotFoundException(
        "User not found with email: " + username
    );
}
```

Spring Security treats this as a failed authentication.

### Step 2. Convert UserRole into GrantedAuthority

Your code:

```java
GrantedAuthority authority =
        new SimpleGrantedAuthority(
            user.getRole().toString()
        );
```

Let's understand this.

Your application has its own enum:

```java
public enum UserRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CASHIER,
    ROLE_BRANCH_MANAGER,
    ROLE_STORE_MANAGER,
    ROLE_STORE_ADMIN,
    ROLE_STORE_EMPLOYEE,
    ROLE_BRANCH_CASHIER
}
```

But Spring Security works with `GrantedAuthority`.

So we convert the role into a Spring Security authority.

For example:

```text
UserRole:
ROLE_STORE_ADMIN
       |
       v
SimpleGrantedAuthority
       |
       v
GrantedAuthority:
ROLE_STORE_ADMIN
```

What is SimpleGrantedAuthority?

It is a built-in Spring Security implementation of the `GrantedAuthority` interface.

It represents a single authority or permission.

### Step 3. Store authorities in a collection

Your code:

```java
Collection<GrantedAuthority> authorities =
        Collections.singletonList(authority);
```

Why do we need a collection?

Because a user may have multiple authorities.

For example:

```java
ROLE_ADMIN
ROLE_MANAGER
ROLE_CASHIER
```

Your current implementation assigns only one role to each user, so you use:

```java
Collections.singletonList()
```

This creates an immutable list containing exactly one element.

### Step 4. Return UserDetails

Your code:

```java
return new org.springframework.security.core.userdetails.User(
    user.getEmail(),
    user.getPassword(),
    authorities
);
```

This is very important.

Notice that this is not your application's `User` entity.

This is Spring Security's built-in User implementation.

We are creating a Spring Security UserDetails object.

It contains:

| Parameter   | Value              |
| ----------- | ------------------ |
| Username    | User email         |
| Password    | Stored BCrypt hash |
| Authorities | User roles         |

Why return this object?

Because Spring Security understands the `UserDetails` interface.

It can use this object to authenticate the user and later access their roles.

> [!TIP]
> **Interview Question:** Why do we implement UserDetailsService?
>
> **Answer:** To load user information from our application's database and provide it to Spring Security in the form of a UserDetails object.


---

# 7. UserRepository.java

Now let's understand how the database query actually happens.

Your code:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    User findByEmail(String email);
}
```

First, what is JpaRepository?

It is a Spring Data JPA interface.

It provides common database operations without requiring us to write SQL manually.

For example:

```java
userRepository.save(user);

userRepository.findById(1L);

userRepository.findAll();

userRepository.deleteById(1L);
```

Now understand:

```java
JpaRepository<User, Long>
```

There are two generic parameters.

- `User` = Entity type.
- `Long` = Primary key type.

Your User entity has:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Therefore, the primary key type is Long.

### What is findByEmail()?

```java
User findByEmail(String email);
```

Spring Data JPA supports query derivation from method names.

It understands:

- `findBy` = retrieve a record.
- `Email` = use the email property as the condition.

Conceptually:

```sql
SELECT *
FROM users
WHERE email = ?;
```

You don't need to write the query yourself.

> [!TIP]
> **Interview Question:** What is the advantage of Spring Data JPA?
>
> **Answer:** It reduces boilerplate database code by providing repository interfaces and automatically generating queries from method names.


---

# 8. User.java – Understanding the Entity

Now let's connect the database with Java.

Your code:

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
```

### What is @Entity?

It tells JPA that this Java class represents a database entity.

Hibernate maps it to a database table.

For example:

```text
Java class: User
      |
      v
Database table: users
```

### What is @Table(name = "users")?

```java
@Table(name = "users")
```

Specifies the database table name.

### What is @Id?

```java
@Id
private Long id;
```

Identifies the primary key.

### What is @GeneratedValue?

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

Tells the database to generate the primary key automatically when inserting a new record.

### What is @Column?

Example:

```java
@Column(nullable = false, unique = true)
private String email;
```

- `nullable = false` means the database column cannot contain NULL.
- `unique = true` means duplicate email values are not allowed.

### What is @Enumerated(EnumType.STRING)?

Your code:

```java
@Enumerated(EnumType.STRING)
private UserRole role;
```

Remember UserRole is an enum.

We want to store the enum name in the database.

For example:

```text
ROLE_STORE_ADMIN
```

Instead of storing an ordinal number such as:

```text
5
```

Why is STRING useful?

Because enum ordinal positions can change when you reorder enum constants. Storing the name makes the database representation easier to understand and avoids that particular problem.

### What is @ManyToOne?

Your code:

```java
@ManyToOne
private Store store;

@ManyToOne
private Branch branch;
```

Let's understand with a real example.

One store can have multiple users.

```text
ABC Supermarket
      |
      |---- Kaif
      |---- Ali
      |---- Ahmed
```

This is a many-to-one relationship from the User side.

Many users belong to one store.

Similarly, many users may belong to one branch.

Small correction to the comment in your code: `@ManyToOne` means many User records can reference one Store. It doesn't mean one user can have many stores.


---

# 9. UserRole.java – Understanding Enum

Your code:

```java
public enum UserRole {

    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CASHIER,
    ROLE_BRANCH_MANAGER,
    ROLE_STORE_MANAGER,
    ROLE_STORE_ADMIN,
    ROLE_STORE_EMPLOYEE,
    ROLE_BRANCH_CASHIER;
}
```

What is an enum?

An enum is a Java type that contains a fixed set of predefined constants.

Why do we use it?

Imagine using ordinary strings for roles:

```java
String role = "ADMIN";
```

Someone might accidentally write:

```java
String role = "ADMNI";
```

Java won't automatically know that this is an invalid role.

But with an enum:

```java
UserRole role = UserRole.ROLE_ADMIN;
```

Only predefined enum values are allowed.

### What is @JsonCreator?

Your code also contains:

```java
@JsonCreator
public static UserRole fromString(String value) {
```

This comes from Jackson.

It allows Jackson to convert incoming JSON strings into your enum values using your custom conversion logic.

For example, your method attempts to support:

```java
ROLE_CASHIER
Cashier
CASHIER
```

And normalize them into:

```java
UserRole.ROLE_CASHIER
```

This is useful when different frontend components send role names in different formats.

One important detail: your method uses `value.toUpperCase()`, so its behavior can depend on the default system locale. Using `toUpperCase(Locale.ROOT)` is safer for role normalization.


---

# 10. JwtUtil.java – Connecting with our previous JWT lesson

Now we're back to JWT.

Remember our previous discussion?

We learned:

- JWT generation
- JWT signing
- Claims
- Expiration
- Token parsing
- Signature verification

Your current project has consolidated these operations into `JwtUtil`.

## A. @Component

```java
@Component
public class JwtUtil {
```

This tells Spring to create and manage this class as a bean.

It allows other classes to inject JwtUtil.

## B. SecretKey

```java
private final SecretKey key;
```

This variable stores the cryptographic key used for JWT signing and verification.

Your constructor:

```java
public JwtUtil(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
}
```

Let's understand:

1\. @Value("$")

Spring reads the value of `jwt.secret` from your application configuration or environment.

For example:

```properties
jwt.secret=your-long-secret-key
```

2\. secret.getBytes()

Converts the string into bytes.

3\. Keys.hmacShaKeyFor()

Creates a cryptographic key suitable for HMAC signing.

Now this key is used throughout JwtUtil.

> [!CAUTION]
> **Security Note:** Remember: the key must have sufficient cryptographic strength. Keep it outside source control.

## C. Token expiration

```java
private static final long EXPIRATION =
        1000 * 60 * 60 * 24;
```

Let's calculate:

| Value | Meaning                 |
| ----- | ----------------------- |
| 1000  | Milliseconds per second |
| 60    | Seconds per minute      |
| 60    | Minutes per hour        |
| 24    | Hours per day           |

Total:

```text
86,400,000 milliseconds
```

That's 24 hours.

## D. generateToken()

Your code:

```java
public String generateToken(String email, String role) {

    Map<String, Object> claims = new HashMap<>();

    claims.put("role", role);

    return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(
                new Date(System.currentTimeMillis() + EXPIRATION)
            )
            .signWith(key)
            .compact();
}
```

Let's understand the data flow.

Step 1. Create a Map

```java
Map<String, Object> claims = new HashMap<>();
```

A Map stores key-value pairs.

For example:

```text
role → ROLE_STORE_ADMIN
```

Step 2. Add role

```java
claims.put("role", role);
```

We store the user's role inside the JWT payload.

Step 3. Build JWT

```java
Jwts.builder()
```

Starts creating the token.

Step 4. setClaims()

```java
.setClaims(claims)
```

Adds the custom claims.

Step 5. setSubject()

```java
.setSubject(email)
```

Stores the user's email as the JWT subject.

This is different from the previous JWT implementation we studied, where email was stored as a custom `email` claim.

Step 6. setIssuedAt()

```java
.setIssuedAt(new Date())
```

Stores when the token was created.

Step 7. setExpiration()

```java
.setExpiration(
    new Date(System.currentTimeMillis() + EXPIRATION)
)
```

Sets the expiration time to 24 hours later.

Step 8. signWith()

```java
.signWith(key)
```

Signs the token using your secret key.

Step 9. compact()

```java
.compact();
```

Converts the JWT builder into a compact JWT string.

The token contains:

```json
{
  "sub": "kaif@gmail.com",
  "role": "ROLE_STORE_ADMIN",
  "iat": 1793...,
  "exp": 1793...
}
```

The actual timestamps are Unix timestamps, not the abbreviated illustrative values above.

## E. extractEmail()

```java
public String extractEmail(String token) {
    return extractAllClaims(token).getSubject();
}
```

Remember:

```java
.setSubject(email)
```

We stored the email in the subject.

Now:

```java
.getSubject()
```

retrieves it.

So:

```text
JWT
 |
 v
Extract claims
 |
 v
Get subject
 |
 v
kaif@gmail.com
```

## F. extractRole()

```java
public String extractRole(String token) {
    return (String) extractAllClaims(token).get("role");
}
```

We previously stored:

```java
claims.put("role", role);
```

Now we retrieve that role using:

```java
.get("role")
```

The returned value is converted into a String.

## G. validateToken()

```java
public boolean validateToken(String token, String email) {
    try {
        return extractEmail(token).equals(email)
                && !isExpired(token);
    } catch (Exception e) {
        return false;
    }
}
```

This checks two things.

1. Does the email inside the token match the expected email?
2. Is the token not expired?

If both conditions are true, it returns true.

Otherwise, it returns false.

But there's an important detail: `extractAllClaims()` already verifies the signature when parsing the token. If the signature is invalid, parsing throws an exception and your method returns false.

## H. isExpired()

```java
private boolean isExpired(String token) {
    return extractAllClaims(token)
            .getExpiration()
            .before(new Date());
}
```

This checks whether the token's expiration time is before the current time.

For example:

```text
Token expiry: 10:00 AM
Current time: 11:00 AM
```

The token has expired.

## I. extractAllClaims()

Your code:

```java
private Claims extractAllClaims(String token) {

    return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
}
```

This is the actual token parsing and signature verification logic in your current JJWT API version.

- `parserBuilder()` creates a parser builder.
- `setSigningKey(key)` provides the verification key.
- `build()` builds the parser.
- `parseClaimsJws(token)` parses the signed token and verifies its signature.
- `getBody()` returns the JWT claims.

This method is used by `extractEmail()`, `extractRole()`, and `isExpired()`.

Important distinction between your two JWT implementations:

| Earlier JwtProvider           | Current JwtUtil               |
| ----------------------------- | ----------------------------- |
| `claim("email", email)`       | `setSubject(email)`           |
| `claim("authorities", roles)` | `setClaims(claims)` with role |
| `get("email")`                | `getSubject()`                |
| `get("authorities")`          | `get("role")`                 |

Both generate signed JWTs, but they use different claim structures.


---

# 11. AuthResponse.java

Your code:

```java
@Data
@Component
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;
}
```

This is the object returned to the frontend after successful login or signup.

Let's understand the fields.

1\. jwt

The token that the frontend uses for subsequent authenticated API requests.

2\. message

A success message.

3\. user

The user's profile information.

Why do we use `UserDto` instead of `User`?

Because the database entity contains sensitive and internal information.

For example:

```java
private String password;
```

We shouldn't expose the password hash to the frontend.

We convert the entity into a DTO through:

```java
UserMapper.toDto(user);
```

One small improvement: `@Component` is not necessary on this response DTO. You create it using `new AuthResponse()`, so it doesn't need to be a Spring-managed bean.


---

# 12. The complete login flow — Let's connect everything

This is the most important revision section.

Imagine Kaif logs into the POS application.

## Complete login execution

1. Frontend sends LoginRequest
   ```json
   {
     "email": "kaif@gmail.com",
     "password": "password123"
   }
   ```
2. AuthController receives request

   `@RequestBody` converts JSON into LoginRequest. `@Valid` checks the fields.
3. AuthenticationManager authenticates

   Receives an unauthenticated UsernamePasswordAuthenticationToken.
4. DaoAuthenticationProvider calls CustomUserImplementation

   Loads the user by email.
5. UserRepository queries MySQL

   Retrieves the User entity.
6. UserDetails is created

   Email, BCrypt hash and GrantedAuthority are returned to Spring Security.
7. PasswordEncoder verifies password

   Checks the submitted password against the stored hash.
8. Authentication succeeds

   AuthenticationManager returns an authenticated Authentication object.
9. AuthController retrieves User

   Calls `userRepository.findByEmail()` again.
10. Updates last login

    Saves the updated User entity.
11. JwtUtil generates token

    Stores email, role, issue time and expiration; signs the JWT.
12. AuthResponse is created

    JWT + success message + UserDto.
13. Response returned

    HTTP 200 OK.


---

# 13. Important difference: Your previous AuthServiceImp

You also shared this earlier:

```java
private Authentication authenticate(
        String email,
        String password
) {
    UserDetails userDetails =
            customUserImplementation.loadUserByUsername(email);

    if (passwordEncoder.matches(
            password,
            userDetails.getPassword()
    )) {
        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );
    }

    return null;
}
```

This manually performs authentication.

Your current AuthController instead does:

```java
authManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
```

The difference:

| AuthServiceImp approach              | AuthController approach                      |
| ------------------------------------ | -------------------------------------------- |
| Manually loads UserDetails           | Delegates to AuthenticationManager           |
| Manually checks password             | DaoAuthenticationProvider checks password    |
| Manually creates authenticated token | Provider returns authenticated token         |
| You manage more authentication logic | Spring Security manages authentication logic |

For your current project, we'll continue learning the AuthenticationManager approach. It integrates naturally with Spring Security's authentication architecture.


---

# 14. Important things to verify in your project

I noticed a few implementation details worth checking before moving to the next topic.

| Area              | Observation                                                                                                          |
| ----------------- | -------------------------------------------------------------------------------------------------------------------- |
| Public signup     | Only `ROLE_STORE_ADMIN` is permitted                                                                                 |
| Branch assignment | Current public signup cannot reach the branch-manager logic                                                          |
| Login             | AuthenticationManager result is not stored in a variable, which is fine because you're only checking success/failure |
| Signup            | Does not explicitly establish a Spring Security authenticated context                                                |
| Password          | Correctly hashed with PasswordEncoder                                                                                |
| JWT               | Uses email as subject and role as a custom claim                                                                     |
| AuthResponse      | `@Component` is unnecessary                                                                                          |
| DTOs              | `@Data` makes separate `@Getter` and `@Setter` redundant                                                             |

One particularly important distinction:

Your earlier `AuthServiceImp` manually calls:

```java
SecurityContextHolder.getContext()
        .setAuthentication(authentication);
```

But your current `AuthController` does not.

That's not automatically a problem. Signup generates and returns a JWT, and subsequent requests should authenticate through your JWT filter. The signup request itself does not necessarily need a SecurityContext authentication established.


---

# 15. What files I need next

We've covered the complete conceptual flow using the code you've supplied.

However, I want to inspect three additional pieces before we move to protected requests.

Next files to share

Required for deeper tracing

1\. SecurityConfig.java

I want to see how you configure AuthenticationManager, PasswordEncoder and DaoAuthenticationProvider.

2\. UserMapper.java and UserDto.java

To understand how your User entity becomes a safe frontend response.

3\. JwtFilter.java

To connect the JWT returned during login with the next protected API request.

One thing to remember: The authentication flow is now:

`LoginRequest → AuthController → AuthenticationManager → CustomUserImplementation → UserRepository → PasswordEncoder → JwtUtil → AuthResponse`

Once you understand this flow, you understand how a Spring Boot application authenticates a user using database credentials and JWT.

Next, we'll connect this with the protected request flow, especially how Spring Security knows who the logged-in user is without asking for their password again.