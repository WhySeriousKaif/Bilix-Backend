I will not add this to any file yet. Let’s understand only the first Spring Security configuration shown in the screenshot.

## 1. Why did Spring generate a default password?

After adding this dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Spring Security automatically protects the application.

If we do not provide our own security configuration, Spring Boot:

- protects every endpoint;
- creates a temporary default user;
- prints a generated password in the console;
- enables its default login mechanism.

The instructor did not want this default behavior. Therefore, he created `SecurityConfig`.

### Why does the generated password appear?

The log appears because `spring-boot-starter-security` is on the classpath. Spring Security enables default auto-configuration to protect all endpoints out of the box.

By default, Spring creates:

```text
username = user
password = randomly generated UUID printed in the console
```

This is unrelated to MySQL. Database configuration controls where application data is stored. Spring Security does not automatically load users from the `users` table unless we provide a `UserDetailsService`, authentication provider or custom security configuration.

### Quick local option: fixed default credentials

```properties
spring.security.user.name=admin
spring.security.user.password=admin123
```

This stops the password from changing on every restart, but it is only a simple local-development option.

### Standard API option: custom security configuration

For a JWT API, we replace the default behavior with our own `SecurityFilterChain`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

In the Molla POS project, the final `SecurityConfig` goes further: it defines public endpoints, registers the JWT filter, uses stateless sessions, configures CORS and applies role-based authorization.

---

# 2. Create the SecurityConfig class

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

}
```

## What does `@Configuration` mean?

`@Configuration` tells Spring:

> This class contains configuration instructions and bean definitions.

It means Spring will read this class when the application starts.

A configuration class is normally used to create and configure objects that Spring should manage.

Simple comparison:

```text
Normal Java class
    ↓
You create and manage its objects manually

@Configuration class
    ↓
Spring reads it and manages the objects declared as beans
```

For example:

```java
@Configuration
public class SecurityConfig {
}
```

Spring detects `SecurityConfig` during component scanning.

## What does `@EnableWebSecurity` mean?

`@EnableWebSecurity` tells Spring:

> Enable Spring Security’s web-security system and use my security configuration.

It activates the infrastructure needed to secure HTTP requests.

In simple words:

```text
@Configuration 
      ↓
This is a Spring configuration class

@EnableWebSecurity
      ↓
This configuration is specifically for web security
```

With modern Spring Boot, much of Spring Security is automatically configured when the dependency is present. Therefore, `@EnableWebSecurity` is sometimes not strictly required, but it makes the purpose explicit.

Interview answer:

> `@Configuration` marks the class as a source of Spring bean definitions. `@EnableWebSecurity` enables and customizes Spring Security for web requests.

---

# 3. What is a Spring bean?

You remembered it correctly: `@Bean` is used to create a custom bean.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    return http.build();
}
```

## What exactly is a bean?

A bean is simply an object created and managed by Spring.

Without Spring:

```java
SecurityFilterChain chain = new SecurityFilterChain();
```

You create and manage the object.

With Spring:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    return http.build();
}
```

Spring:

1. calls this method;
2. receives the returned object;
3. stores that object inside the Spring container;
4. manages its lifecycle;
5. makes it available to other parts of the application.

So, `@Bean` does not mean “create a Java class.”

It means:

> Execute this method and register its returned object inside the Spring container.

By default, Spring creates one shared instance of this bean. This is called singleton scope.

## Why is `SecurityFilterChain` a bean?

Spring Security looks for a `SecurityFilterChain` bean.

That bean tells Spring:

- which URLs are public;
- which URLs require login;
- which roles can access which URLs;
- whether sessions should be created;
- which security filters should run;
- whether CSRF is enabled;
- how CORS should work.

Think of it as the main rulebook for Spring Security.

```text
SecurityFilterChain
        ↓
A collection of security rules and filters
        ↓
Applied to incoming HTTP requests
```

---

# 4. Where does `HttpSecurity` come from?

Look at this parameter:

```java
public SecurityFilterChain securityFilterChain(
        HttpSecurity http
) throws Exception {
```

We did not write:

```java
HttpSecurity http = new HttpSecurity();
```

Spring provides it automatically.

This is dependency injection.

Spring sees that the method needs an `HttpSecurity` object, so Spring passes the configured object into the method.

```text
Spring creates HttpSecurity
        ↓
Spring passes it to our @Bean method
        ↓
We use it to define security rules
        ↓
http.build() creates SecurityFilterChain
```

## What is `HttpSecurity`?

`HttpSecurity` is a builder.

We use it to build our web-security configuration.

For example:

```java
http
    .csrf(csrf -> csrf.disable())
    .authorizeHttpRequests(auth -> auth
        .anyRequest().authenticated()
    );

return http.build();
```

The `http` object collects all these instructions.

Finally:

```java
http.build();
```

converts the collected rules into a `SecurityFilterChain`.

---

# 5. First complete configuration

Start with the smallest possible example:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
        );

        return http.build();
    }
}
```

Meaning:

```text
Every request
      ↓
User must be authenticated
```

Now every endpoint requires authentication.

---

# 6. Make the application stateless

The transcript then adds:

```java
http.sessionManagement(session ->
    session.sessionCreationPolicy(
        SessionCreationPolicy.STATELESS
    )
);
```

Complete example:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .sessionManagement(session ->
            session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            )
        )
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        );

    return http.build();
}
```

## What does `STATELESS` mean?

It means:

> The backend will not create a server-side login session for the user.

In a traditional session system:

```text
User logs in
    ↓
Server creates a session
    ↓
Server stores session information
    ↓
Browser sends session ID
```

In this JWT system:

```text
User logs in
    ↓
Backend generates JWT
    ↓
Frontend stores JWT
    ↓
Frontend sends JWT with every request
    ↓
Backend validates JWT again
```

Therefore:

```java
SessionCreationPolicy.STATELESS
```

tells Spring not to remember login information through an HTTP session.

Important clarification:

Stateless does not mean the application has no database.

It only means:

> The server does not keep a login session between requests.

---

# 7. Configure public and protected endpoints

A simple configuration could be:

```java
http.authorizeHttpRequests(auth -> auth

    // Public endpoints
    .requestMatchers("/auth/**").permitAll()

    // Only ADMIN can access
    .requestMatchers("/api/super-admin/**")
        .hasRole("ADMIN")

    // Every other /api endpoint needs authentication
    .requestMatchers("/api/**")
        .authenticated()

    // Other requests are public
    .anyRequest()
        .permitAll()
);
```

## What does `requestMatchers()` mean?

It selects requests based on the URL.

```java
.requestMatchers("/auth/**")
```

means:

```text
/auth/login
/auth/signup
/auth/anything
```

The `/**` part means everything under that path.

## What does `permitAll()` mean?

```java
.requestMatchers("/auth/**").permitAll()
```

means:

> Anyone can access these endpoints. A JWT is not required.

Login and registration must usually be public because the user does not have a JWT before logging in.

## What does `authenticated()` mean?

```java
.requestMatchers("/api/**").authenticated()
```

means:

> The user must be successfully authenticated before accessing these endpoints.

It does not specify a particular role.

Any authenticated user may pass this rule unless another rule or service-level check restricts them.

## What does `hasRole("ADMIN")` mean?

```java
.requestMatchers("/api/super-admin/**")
    .hasRole("ADMIN")
```

means:

> Only a user with the `ROLE_ADMIN` authority may access these endpoints.

Notice that we write:

```java
hasRole("ADMIN")
```

not:

```java
hasRole("ROLE_ADMIN")
```

Spring automatically adds the `ROLE_` prefix when using `hasRole`.

Therefore:

```java
hasRole("ADMIN")
```

checks for:

```text
ROLE_ADMIN
```

---

# 8. Very important correction in the screenshot

The screenshot appears to put this rule first:

```java
.requestMatchers("/api/**")
    .authenticated()
```

and the more specific rule after it:

```java
.requestMatchers("/api/super-admin/**")
    .hasRole("ADMIN")
```

That ordering is dangerous.

Spring Security uses the first matching rule.

The URL:

```text
/api/super-admin/dashboard
```

matches both patterns:

```text
/api/**
/api/super-admin/**
```

If `/api/**` appears first, Spring may stop there and only require authentication. The later admin rule may never be used.

The safer order is:

```java
.authorizeHttpRequests(auth -> auth

    // Most specific rule first
    .requestMatchers("/api/super-admin/**")
        .hasRole("ADMIN")

    // More general rule after it
    .requestMatchers("/api/**")
        .authenticated()

    .anyRequest()
        .permitAll()
)
```

Memory rule:

```text
Specific URL rules first
General URL rules later
```

Your final project avoids putting a general `/api/**` rule before the admin rule. It finishes with:

```java
.anyRequest().authenticated()
```

after the specific rules.

---

# 9. Add the JWT filter

The transcript calls it `JwtValidator`. Your final project calls it `JwtFilter`.

The purpose is the same:

```text
Read JWT
   ↓
Verify JWT
   ↓
Load user
   ↓
Put authentication into SecurityContextHolder
```

A simplified configuration is:

```java
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtFilter jwtFilter
) throws Exception {

    http
        .sessionManagement(session ->
            session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            )
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/api/super-admin/**")
                .hasRole("ADMIN")
            .anyRequest().authenticated()
        );

    http.addFilterBefore(
        jwtFilter,
        UsernamePasswordAuthenticationFilter.class
    );

    return http.build();
}
```

## What does `addFilterBefore()` mean?

```java
http.addFilterBefore(
    jwtFilter,
    UsernamePasswordAuthenticationFilter.class
);
```

It means:

> Run my JWT filter before Spring’s username/password authentication filter.

The order becomes approximately:

```text
Incoming request
      ↓
JwtFilter
      ↓
UsernamePasswordAuthenticationFilter
      ↓
Authorization rules
      ↓
Controller
```

Why should `JwtFilter` run first?

Because Spring needs to know the authenticated user before checking:

```java
.authenticated()
```

or:

```java
.hasRole("ADMIN")
```

The JWT filter places the authentication inside `SecurityContextHolder`. Spring Security then uses that authentication to make its decision.

## Transcript versus current project

The transcript uses:

```java
new JwtValidator()
```

Your final project uses dependency injection:

```java
public SecurityFilterChain chain(
        HttpSecurity http,
        JwtFilter filter
)
```

and:

```java
http.addFilterBefore(
    filter,
    UsernamePasswordAuthenticationFilter.class
);
```

Your current approach is better because `JwtFilter` needs dependencies such as:

```java
JwtUtil
CustomUserImplementation
```

Spring creates the filter and injects those dependencies.

If we manually write:

```java
new JwtFilter()
```

Spring cannot automatically inject its required dependencies unless we provide them ourselves.

---

# 10. Disable CSRF

The transcript uses:

```java
http.csrf(csrf -> csrf.disable());
```

Your project uses the same idea:

```java
http.csrf(cs -> cs.disable());
```

## What is CSRF?

CSRF means Cross-Site Request Forgery.

It is mainly dangerous when the browser automatically sends authentication information, especially session cookies.

Example:

```text
User logs into a bank website
        ↓
Browser stores the bank session cookie
        ↓
User visits a malicious website
        ↓
Malicious website sends a bank request
        ↓
Browser automatically includes the bank cookie
```

## Why is it disabled here?

This application uses a stateless JWT sent manually in the header:

```http
Authorization: Bearer <token>
```

Another website cannot normally force the browser to attach this custom authorization header automatically.

Therefore, stateless bearer-token APIs often disable CSRF:

```java
.csrf(csrf -> csrf.disable())
```

Important correction:

Do not memorize:

> JWT means CSRF is always disabled.

A better answer is:

> CSRF is disabled here because authentication uses a stateless bearer token in the Authorization header. If authentication is moved into cookies, CSRF protection must be reconsidered.

---

# 11. Configure CORS

The frontend and backend normally run on different origins.

For example:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:5001
```

Even though both use `localhost`, the ports are different. Therefore, they are different origins.

The browser may block the frontend from calling the backend unless the backend allows that origin.

Security configuration:

```java
http.cors(cors ->
    cors.configurationSource(corsConfigurationSource())
);
```

Then define the CORS rules:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration config =
        new CorsConfiguration();

    config.setAllowedOrigins(
        List.of("http://localhost:5173")
    );

    config.setAllowedMethods(
        List.of(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        )
    );

    config.setAllowedHeaders(
        List.of("*")
    );

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
        "/**",
        config
    );

    return source;
}
```

Simple meaning:

```text
CORS tells the browser:
“This frontend origin is allowed to call this backend.”
```

CORS does not authenticate the user.

CORS and JWT solve different problems:

```text
CORS
    ↓
Is this website origin allowed to call the backend?

JWT
    ↓
Who is the user making the request?

Authorization
    ↓
Is this user allowed to perform the action?
```

---

# 12. The complete learning version

This is a simplified version of the code you are currently studying:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter
    ) throws Exception {

        http
            // We use JWT, not server-side login sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Decide which URLs are public or protected
            .authorizeHttpRequests(auth -> auth

                // Login and signup are public
                .requestMatchers("/auth/**")
                    .permitAll()

                // Only super admin can access these URLs
                .requestMatchers("/api/super-admin/**")
                    .hasRole("ADMIN")

                // Every other request needs a valid login
                .anyRequest()
                    .authenticated()
            )

            // Disabled for this stateless bearer-token API
            .csrf(csrf -> csrf.disable())

            // Allow the frontend origin to call the backend
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            );

        // Read and validate JWT before Spring's login filter
        http.addFilterBefore(
            jwtFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        // Convert all the instructions into a filter chain
        return http.build();
    }
}
```

---

# 13. How this grows into your final project

The code above is the learning version.

Your final project adds more things:

```text
Basic learning version
    ├── Stateless sessions
    ├── URL authorization
    ├── JWT filter
    ├── CSRF configuration
    └── CORS
          ↓
Current project version
    ├── Public Swagger routes
    ├── Public H2 console
    ├── Public products
    ├── Public subscription plans
    ├── Public payment endpoints
    ├── HTTP-method-specific rules
    ├── Store moderation permissions
    ├── Custom 403 JSON response
    ├── Rate-limiting filter
    ├── Dynamic production origins
    └── H2 frame configuration
```

We should study those additions later, one at a time.

---

# 14. Short interview answers

## What is `@Configuration`?

> It tells Spring that the class contains configuration and bean definitions.

## What is `@EnableWebSecurity`?

> It enables Spring Security’s web-security configuration.

## What is `@Bean`?

> It tells Spring to execute the method and manage the returned object inside the Spring container.

## What is `SecurityFilterChain`?

> It contains the security filters and authorization rules applied to incoming requests.

## What is `HttpSecurity`?

> It is a builder used to configure Spring Security for HTTP requests.

## What does `STATELESS` mean?

> Spring Security will not keep a server-side login session. The JWT must be sent and checked on every request.

## What does `authenticated()` mean?

> Any authenticated user may access the matching endpoint.

## What does `hasRole("ADMIN")` mean?

> The authenticated user must have the `ROLE_ADMIN` authority.

## What does `permitAll()` mean?

> The endpoint can be accessed without authentication.

## Why add `JwtFilter` before another filter?

> The JWT must be validated and the user must be placed in `SecurityContextHolder` before Spring checks authorization rules.

## Why disable CSRF?

> This API uses stateless bearer tokens in the Authorization header instead of cookie-based sessions.

## What is CORS?

> It tells the browser which frontend origins are allowed to call the backend.

---

# 15. Final memory shortcut

```text
@Configuration
    = This class contains Spring setup

@EnableWebSecurity
    = Enable web-security setup

@Bean
    = Let Spring create and manage this object

HttpSecurity
    = Security rule builder

SecurityFilterChain
    = Final security rulebook

STATELESS
    = Do not store login session on server

permitAll()
    = No login required

authenticated()
    = Any logged-in user

hasRole("ADMIN")
    = Only ROLE_ADMIN

JwtFilter
    = Read JWT and set authenticated user

CSRF
    = Disabled for this header-based stateless API

CORS
    = Allow trusted frontend origins
```

---

# 16. How JwtConstant, JwtFilter, JwtUtil and SecurityConfig connect

The first configuration explains which requests are protected.

The next question is:

> How does Spring find and verify the JWT before applying those rules?

These classes work together:

```text
SecurityConfig
    = decides which URLs are protected

JwtFilter
    = reads the token from each request

JwtUtil
    = creates, parses and validates JWTs

JwtConstant
    = older place for JWT-related constant values

SecurityContextHolder
    = keeps the authenticated identity for the current request
```

Complete request flow:

```text
Frontend sends Authorization header
                ↓
Security filter chain receives request
                ↓
JwtFilter reads Bearer token
                ↓
JwtUtil parses and verifies token
                ↓
User is loaded from database
                ↓
Authentication is placed in SecurityContextHolder
                ↓
Spring checks authenticated() or hasRole(...)
                ↓
Controller executes
```

# 17. JwtConstant

The instructor creates a class similar to:

```java
public class JwtConstant {

    public static final String JWT_SECRET = "secret-key";
    public static final String JWT_HEADER = "Authorization";
}
```

## Why use constants?

Without a constant, the same value may be written in many files:

```java
request.getHeader("Authorization");
```

With a constant:

```java
request.getHeader(JwtConstant.JWT_HEADER);
```

Benefits:

- avoids spelling mistakes;
- avoids repeating the same text;
- makes changes easier;
- gives the value a clear name.

## MERN version

```javascript
const JWT_HEADER = "authorization";

const header = req.headers[JWT_HEADER];
```

## Important secret-key rule

Do not keep the real production secret inside Java code:

```java
public static final String JWT_SECRET = "my-real-secret";
```

Anyone who can read the source code can see it.

A better Spring approach is:

```properties
jwt.secret=${JWT_SECRET}
```

```java
public JwtUtil(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
}
```

MERN uses the same idea:

```javascript
const secret = process.env.JWT_SECRET;
```

## What does the current project do?

The current project contains a `JwtConstant` class, but it is not used by `JwtFilter` or `JwtUtil`.

The real key used by the current project comes from:

```java
@Value("${jwt.secret}")
```

Therefore, the current `JwtConstant.JWT_SECRET` is old or duplicate code and could be removed after confirming that nothing else uses it.

The filter currently writes this directly:

```java
request.getHeader("Authorization");
```

Using a header constant is optional. Keeping the secret outside source code is not optional for production security.

# 18. What is OncePerRequestFilter?

The instructor's `JwtValidator` and the current `JwtFilter` extend:

```java
OncePerRequestFilter
```

```java
@Component
public class JwtFilter extends OncePerRequestFilter {
}
```

Simple meaning:

> Run this filter once for each request that passes through the filter chain.

The main method is:

```java
@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException {

}
```

The three parameters mean:

- `request` = incoming HTTP request;
- `response` = outgoing HTTP response;
- `filterChain` = lets the request continue to the next filter.

## MERN version

Express middleware has the same basic job:

```javascript
function jwtMiddleware(req, res, next) {
  // inspect request
  // validate token
  // call next()
}
```

Connection:

```text
OncePerRequestFilter.doFilterInternal(...)
        ≈
Express middleware(req, res, next)
```

# 19. Why is JwtFilter annotated with @Component?

The current filter uses:

```java
@Component
public class JwtFilter extends OncePerRequestFilter {
}
```

`@Component` tells Spring:

> Create and manage an object of this class.

Because Spring manages it, constructor injection works:

```java
public JwtFilter(
        JwtUtil jwtUtil,
        CustomUserImplementation userService
) {
    this.jwtUtil = jwtUtil;
    this.userService = userService;
}
```

Spring provides the `JwtUtil` and user service objects.

This is better than:

```java
new JwtFilter();
```

because the filter needs dependencies.

## MERN version

JavaScript modules commonly receive or import their dependencies:

```javascript
const jwt = require("jsonwebtoken");
const User = require("./models/User");
```

Spring uses its container and dependency injection instead of manually importing and constructing every service object.

# 20. Step 1 — read the Authorization header

Spring:

```java
String authHeader = request.getHeader("Authorization");
```

MERN:

```javascript
const authHeader = req.headers.authorization;
```

Example header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The word `Bearer` describes the authentication scheme.

Simple meaning:

> Whoever bears or possesses this valid token can present it for authentication.

# 21. Step 2 — check Bearer and extract the token

Spring:

```java
if (authHeader != null
        && authHeader.trim().startsWith("Bearer ")) {

    String token = authHeader.substring(7).trim();
}
```

MERN:

```javascript
if (authHeader?.startsWith("Bearer ")) {
  const token = authHeader.split(" ")[1];
}
```

Why `substring(7)`?

```text
Bearer␠
1234567
```

It removes:

- six letters in `Bearer`;
- one space.

Only the JWT remains.

## Where is this logic written in the project?

There is currently no separate method named `trimJwt()` or `extractBearerToken()`.

The cleanup logic is written directly in two places:

```text
1. UserServiceImp.getUserFromJwt(String jwt)
2. JwtFilter.doFilterInternal(...)
```

In `UserServiceImp.getUserFromJwt()`:

```java
String cleanToken = jwt != null ? jwt.trim() : "";

if (cleanToken.startsWith("Bearer ")) {
    cleanToken = cleanToken.substring(7).trim();
}
```

Meaning of each Java method:

```text
trim()
    = removes extra spaces at the beginning and end

startsWith("Bearer ")
    = checks that the header uses the Bearer scheme

substring(7)
    = removes the seven characters in "Bearer "
```

After this code, `cleanToken` contains only the JWT:

```text
Bearer eyJhbGciOiJIUzI1NiJ9...
       ↓
eyJhbGciOiJIUzI1NiJ9...
```

The service then passes the clean token to:

```java
String email = jwtUtil.extractEmail(cleanToken);
```

The same header-cleaning idea appears in `JwtFilter`:

```java
final String authHeader = request.getHeader("Authorization");

if (authHeader != null && authHeader.trim().startsWith("Bearer ")) {
    token = authHeader.substring(7).trim();
}
```

So the current project has this small piece of logic duplicated. A future cleanup could move it into one shared helper method, but the current code works by performing it inline.

## MERN comparison

MERN commonly removes the prefix like this:

```javascript
const token = req.headers.authorization
    ?.split(" ")[1];
```

Both implementations do the same thing:

```text
Read Authorization header
        ↓
Check Bearer prefix
        ↓
Remove Bearer and the space
        ↓
Keep only the JWT
```

# 22. Step 3 — parse and verify the token

The current project calls:

```java
email = jwtUtil.extractEmail(token);
```

Inside `JwtUtil`:

```java
private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
}
```

This does two important things:

```text
Parse
    = read the token data

Verify
    = check that the signature matches the secret key
```

If someone changes the payload, the signature no longer matches.

## MERN version

```javascript
const decoded = jwt.verify(
  token,
  process.env.JWT_SECRET
);
```

Do not confuse `verify()` with `decode()`.

```javascript
jwt.decode(token);
```

can read the payload without proving that the token is genuine.

```javascript
jwt.verify(token, secret);
```

checks the signature and expiration.

Memory rule:

```text
Decode = read
Verify = trust only after signature check
```

# 23. What are JWT claims?

Claims are pieces of information stored inside the JWT payload.

The current project creates claims like:

```json
{
  "sub": "kaif@example.com",
  "role": "ROLE_CASHIER",
  "iat": 1700000000,
  "exp": 1700086400
}
```

Meanings:

- `sub` = subject, which identifies the user;
- `role` = custom role claim;
- `iat` = token issue time;
- `exp` = token expiration time.

The current project stores the email as the subject:

```java
.setSubject(email)
```

It reads the email using:

```java
claims.getSubject()
```

## Instructor version versus current version

The instructor stores email using a custom claim:

```java
.claim("email", authentication.getName())
```

and reads it with:

```java
claims.get("email")
```

Your current project uses the standard subject claim instead:

```java
.setSubject(email)
```

```java
claims.getSubject()
```

Both can work, but the code that creates and reads the token must agree on the claim name.

## MERN version

```javascript
const token = jwt.sign(
  {
    role: user.role
  },
  process.env.JWT_SECRET,
  {
    subject: user.email,
    expiresIn: "24h"
  }
);
```

Reading the subject:

```javascript
const decoded = jwt.verify(token, secret);
const email = decoded.sub;
```

# 24. Step 4 — load the current user from the database

The current project does this:

```java
UserDetails userDetails =
        userService.loadUserByUsername(email);
```

Why load the user when the token already contains an email and role?

Because the database contains the current user information.

For example:

- the user may have been deleted;
- the role may have changed;
- the account may later be disabled or blocked;
- the authorities inside an old token may be outdated.

The same idea is used by `UserServiceImp.getUserFromJwt(String jwt)` when a controller needs the complete `User` object:

```java
String email = jwtUtil.extractEmail(cleanToken);
User user = userRepository.findByEmail(email);

if (user == null) {
    throw new NotFoundException("User not found");
}

return user;
```

This is a two-step process, not one operation:

```text
JwtUtil
    = verifies/parses the JWT and extracts the email

UserRepository
    = uses that email to fetch the complete current user from SQL
```

The JWT identifies the user; it usually does not contain the complete database record. That is why the application extracts the email first and then performs `findByEmail(email)`.

## MERN version

```javascript
const decoded = jwt.verify(token, secret);

const user = await User.findOne({
  email: decoded.sub
});

if (!user) {
  return res.status(401).json({
    message: "User not found"
  });
}
```

# 25. Instructor validator versus current JwtFilter

The instructor's validator reads authorities directly from JWT claims:

```java
String authorities =
        String.valueOf(claims.get("authorities"));

List<GrantedAuthority> auths =
        AuthorityUtils
            .commaSeparatedStringToAuthorityList(authorities);
```

It then creates authentication using those token authorities.

The current project does something different:

```java
UserDetails userDetails =
        userService.loadUserByUsername(email);

userDetails.getAuthorities();
```

## Trade-off

Using token authorities:

- avoids a database query on every request;
- is faster;
- keeps authentication fully stateless;
- but an old token may keep an old role until it expires.

Loading authorities from the database:

- uses the current role;
- can notice deleted or changed users;
- but adds a database query to authenticated requests.

The current project chooses fresher database authorities over avoiding the lookup.

## MERN comparison

Trust role from JWT:

```javascript
const decoded = jwt.verify(token, secret);

req.user = {
  email: decoded.sub,
  role: decoded.role
};
```

Load current role from database:

```javascript
const decoded = jwt.verify(token, secret);
const user = await User.findOne({ email: decoded.sub });

req.user = {
  email: user.email,
  role: user.role
};
```

# 26. Step 5 — validate email and expiration

The current project uses:

```java
if (jwtUtil.validateToken(
        token,
        userDetails.getUsername()
)) {
    // create authentication
}
```

The method checks:

```java
return extractEmail(token).equals(email)
        && !isExpired(token);
```

Simple meaning:

```text
Token belongs to this email
            AND
Token has not expired
```

The JJWT parser also verifies the signature when it parses the claims.

## MERN version

```javascript
const decoded = jwt.verify(token, secret);

if (decoded.sub !== user.email) {
  return res.status(401).json({
    message: "Invalid token user"
  });
}
```

`jwt.verify()` automatically rejects an expired token.

# 27. Step 6 — create the Authentication object

Current Spring code:

```java
UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
```

Meanings:

```text
userDetails
    = principal, or who the user is

null
    = password is not required again

authorities
    = roles or permissions
```

The password was checked during login. This request is being authenticated through its JWT.

## MERN version

```javascript
req.user = {
  id: user._id,
  email: user.email,
  role: user.role
};
```

Express does not require a standard `Authentication` class. Spring Security provides one standard security model.

# 28. WebAuthenticationDetailsSource

The current filter also uses:

```java
authToken.setDetails(
    new WebAuthenticationDetailsSource()
        .buildDetails(request)
);
```

This adds extra request details to the authentication object, such as the remote address and session-related request information.

It does not create the user's identity or role. Those come from `userDetails`.

## MERN comparison

Express already exposes request information:

```javascript
const ip = req.ip;
const userAgent = req.headers["user-agent"];
```

You could attach it yourself:

```javascript
req.authDetails = {
  ip: req.ip,
  userAgent: req.headers["user-agent"]
};
```

# 29. Step 7 — store authentication in SecurityContextHolder

```java
SecurityContextHolder
        .getContext()
        .setAuthentication(authToken);
```

Simple meaning:

> This request now has an authenticated user.

Spring Security can now evaluate:

```java
.authenticated()
```

and:

```java
.hasRole("ADMIN")
```

## MERN version

```javascript
req.user = user;
```

Connection:

```text
Express req.user
        ≈
Spring SecurityContextHolder authentication
```

# 29A. Why does `getAuthentication().getName()` return the email?

Your `getCurrentUser()` method does this:

```java
String email = SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getName();

User user = userRepository.findByEmail(email);
```

The email is available because it was placed into the authentication object earlier in the filter flow.

In your project, `CustomUserImplementation` creates `UserDetails` like this:

```java
return new org.springframework.security.core.userdetails.User(
    user.getEmail(),
    user.getPassword(),
    authorities
);
```

The first argument is called `username` by Spring Security, but your application uses the email as the username.

The complete chain is:

```text
JWT contains email
        ↓
JwtUtil extracts email
        ↓
UserDetailsService loads the user using that email
        ↓
UserDetails stores email as its username
        ↓
UsernamePasswordAuthenticationToken stores UserDetails as principal
        ↓
SecurityContextHolder stores the authentication
        ↓
authentication.getName() returns UserDetails.getUsername()
        ↓
In this project, that username is the email
```

So `.getName()` does not search the database and does not read the JWT directly. It reads the name of the principal already stored in the current authentication object.

In this project:

```text
authentication.getName()
    = userDetails.getUsername()
    = user.getEmail()
```

After getting the email, `UserRepository.findByEmail(email)` fetches the complete and current `User` entity from the database.

## MERN comparison

Express middleware verifies the token and attaches the user to the request:

```javascript
const decoded = jwt.verify(token, process.env.JWT_SECRET);

req.user = await User.findOne({
  email: decoded.sub
});

next();
```

Later handlers can use:

```javascript
const email = req.user.email;
```

Spring uses a standard security object instead of a custom `req.user` property:

```java
String email = SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getName();
```

The matching idea is:

```text
Express: req.user.email
Spring:  authentication.getName()
```

Both read identity information that authentication middleware already established for the current request.

# 30. Why check whether authentication is already present?

The current filter checks:

```java
SecurityContextHolder
    .getContext()
    .getAuthentication() == null
```

This means:

> Do not authenticate the request again if another filter has already done it.

## MERN comparison

```javascript
if (!req.user) {
  req.user = await authenticateToken(req);
}
```

# 31. Step 8 — continue the filter chain

Spring:

```java
filterChain.doFilter(request, response);
```

Express:

```javascript
next();
```

Both mean:

> My middleware/filter work is finished. Continue processing the request.

If this line is not called and no response is sent, the request stops and may appear to hang.

# 32. What happens when the token is missing?

In the current filter, if there is no Bearer token:

```text
No authentication is placed in SecurityContextHolder
                        ↓
Filter chain continues
                        ↓
Public endpoint may still work
                        ↓
Protected endpoint is rejected by Spring Security
```

This is useful because the same filter also sees public requests such as login.

## MERN version

The middleware is normally attached only to protected routes:

```javascript
app.get(
  "/profile",
  jwtMiddleware,
  profileController
);
```

Or it can be global and skip public paths.

# 33. What happens when the token is invalid?

The current `JwtFilter` catches token errors and continues without authentication.

For a protected URL, Spring later rejects the request because the user is not authenticated.

For a public URL, the request may continue normally.

This design helps public routes work even if the browser sends an old token.

However, a stricter production design may immediately return `401 Unauthorized` when the client explicitly sends a malformed or invalid Bearer token.

## MERN strict version

```javascript
try {
  const decoded = jwt.verify(token, secret);
  req.user = decoded;
  next();
} catch (error) {
  return res.status(401).json({
    message: "Invalid or expired token"
  });
}
```

# 34. 401 versus 403

```text
401 Unauthorized
    = You are not successfully authenticated

403 Forbidden
    = We know who you are, but you do not have permission
```

Examples:

```text
Missing or invalid JWT
    → 401

Valid cashier JWT accessing super-admin endpoint
    → 403
```

MERN:

```javascript
if (!req.user) {
  return res.status(401).json({
    message: "Login required"
  });
}

if (req.user.role !== "ROLE_ADMIN") {
  return res.status(403).json({
    message: "Access denied"
  });
}
```

Spring Security performs the same decision through authentication and authorization rules.

# 35. JwtUtil creates the token

The current project creates a token using:

```java
public String generateToken(
        String email,
        String role
) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", role);

    return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(
                new Date(
                    System.currentTimeMillis()
                    + EXPIRATION
                )
            )
            .signWith(key)
            .compact();
}
```

MERN version:

```javascript
const token = jwt.sign(
  {
    role: user.role
  },
  process.env.JWT_SECRET,
  {
    subject: user.email,
    expiresIn: "24h"
  }
);
```

Method connection:

```text
Jwts.builder()
    ≈ prepare jwt.sign()

setSubject(email)
    ≈ subject: user.email

setExpiration(...)
    ≈ expiresIn: "24h"

signWith(key)
    ≈ JWT_SECRET

compact()
    ≈ return final token string
```

# 36. The JWT is signed, not normally encrypted

The current project creates a signed JWT.

Its payload can be decoded by someone who has the token.

The signature prevents someone from changing the payload without detection.

Therefore, do not store secrets inside claims:

```json
{
  "password": "never-put-this-here"
}
```

Safe claims are normally small identity or authorization values:

```json
{
  "sub": "kaif@example.com",
  "role": "ROLE_CASHIER"
}
```

This rule is the same in MERN and Spring Boot.

# 37. How SecurityConfig uses JwtFilter

Current project:

```java
@Bean
public SecurityFilterChain chain(
        HttpSecurity http,
        JwtFilter filter
) throws Exception {

    http.addFilterBefore(
        filter,
        UsernamePasswordAuthenticationFilter.class
    );

    return http.build();
}
```

Spring injects the `JwtFilter` bean into this method.

The filter runs before Spring's username/password filter so the security context is ready before authorization is checked.

MERN version:

```javascript
app.get(
  "/profile",
  jwtMiddleware,
  profileController
);
```

Middleware/filter order matters in both frameworks.

# 38. RateLimitingFilter in the current project

The final project also adds:

```java
http.addFilterBefore(
    new RateLimitingFilter(),
    JwtFilter.class
);
```

Order:

```text
Request
   ↓
RateLimitingFilter
   ↓
JwtFilter
   ↓
UsernamePasswordAuthenticationFilter
   ↓
Controller
```

Why rate limiting first?

It can reject excessive traffic before doing JWT parsing and database work.

MERN version:

```javascript
app.use(rateLimitMiddleware);
app.use(jwtMiddleware);
```

Important production note: the current rate limiter is created using `new RateLimitingFilter()`. If it later needs Spring dependencies or shared distributed state, it should be managed as a Spring bean. A local in-memory limiter also does not automatically share limits between multiple application instances.

# 39. PasswordEncoder is related but does not validate JWTs

SecurityConfig also creates:

```java
@Bean
public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt is used for passwords:

```java
String hash = passwordEncoder.encode(password);
```

During login:

```java
passwordEncoder.matches(
    rawPassword,
    storedHash
);
```

It is separate from JWT signing.

```text
BCrypt
    = hash and verify passwords

JJWT
    = create and verify JWTs
```

Important correction to the transcript:

BCrypt does not encrypt or decrypt passwords.

It hashes them using a one-way algorithm.

## MERN version

```javascript
const hash = await bcrypt.hash(password, 12);

const matches = await bcrypt.compare(
  enteredPassword,
  storedHash
);
```

# 40. Complete Spring and MERN connection

Spring Boot:

```java
String header = request.getHeader("Authorization");
String token = header.substring(7);

String email = jwtUtil.extractEmail(token);

UserDetails user =
    userService.loadUserByUsername(email);

if (jwtUtil.validateToken(token, user.getUsername())) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            user,
            null,
            user.getAuthorities()
        );

    SecurityContextHolder
        .getContext()
        .setAuthentication(auth);
}

filterChain.doFilter(request, response);
```

MERN:

```javascript
const header = req.headers.authorization;
const token = header.split(" ")[1];

const decoded = jwt.verify(
  token,
  process.env.JWT_SECRET
);

const user = await User.findOne({
  email: decoded.sub
});

req.user = {
  id: user._id,
  email: user.email,
  role: user.role
};

next();
```

# 41. Interview answer to memorize

> My `SecurityConfig` defines which endpoints are public or protected and places the JWT filter before Spring's username/password filter. The `JwtFilter`, which extends `OncePerRequestFilter`, reads the Bearer token from the Authorization header. `JwtUtil` verifies the signature, extracts the email and checks expiration. The filter then loads the current user from the database, creates a `UsernamePasswordAuthenticationToken` with the user's authorities and stores it in `SecurityContextHolder`. Spring Security then uses that authentication for rules such as `authenticated()` and `hasRole()`. This is similar to Express JWT middleware verifying a token, loading the user, assigning `req.user` and calling `next()`.

# 42. Final memory shortcut

```text
JwtConstant
    = reusable JWT text values
    = do not hard-code production secret

JwtUtil
    = generate, parse and validate JWT

JwtFilter
    = inspect every request

OncePerRequestFilter
    = run filter once per request

Authorization header
    = carries Bearer token

substring(7)
    = removes "Bearer "

Claims
    = information inside JWT payload

UserDetailsService
    = loads current user from database

Authentication
    = principal + authorities

SecurityContextHolder
    = stores authentication for current request

filterChain.doFilter(...)
    = Express next()

401
    = not authenticated

403
    = authenticated but not allowed
```

# 43. What is `JwtProvider`?

In the tutorial, `JwtProvider` is the class that **creates the JWT after the user has successfully logged in**.

Its jobs are:

1. Take the authenticated user's identity.
2. Take the user's roles or authorities.
3. Put that information inside the JWT.
4. Add the creation and expiry times.
5. Sign the JWT with the secret key.
6. Return the completed token.

Important:

> `JwtProvider` does not check the email and password itself.

`AuthenticationManager` checks the credentials first. The provider creates a token only after authentication succeeds.

MERN comparison:

```javascript
const token = jwt.sign(
  { email: user.email, role: user.role },
  process.env.JWT_SECRET,
  { expiresIn: "24h" }
);
```

`jwt.sign(...)` performs the same main job as the tutorial's `JwtProvider`.

# 44. Simplified tutorial `JwtProvider`

```java
@Service
public class JwtProvider {

    private final SecretKey key;

    public String generateJwtToken(Authentication authentication) {

        String authorities = populateAuthorities(
            authentication.getAuthorities()
        );

        return Jwts.builder()
            .setIssuedAt(new Date())
            .setExpiration(
                new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
            )
            .claim("email", authentication.getName())
            .claim("authorities", authorities)
            .signWith(key)
            .compact();
    }
}
```

Simple flow:

```text
Authentication object
        ↓
Get email and authorities
        ↓
Put them inside JWT claims
        ↓
Sign the JWT
        ↓
Return the token
```

The exact JJWT methods may be slightly different between library versions. The idea is the same.

# 45. What does `populateAuthorities()` do?

Spring stores authorities as a collection:

```java
Collection<? extends GrantedAuthority>
```

For example:

```text
[ROLE_ADMIN, ROLE_CASHIER]
```

The tutorial converts that collection into one string:

```text
ROLE_ADMIN,ROLE_CASHIER
```

```java
private String populateAuthorities(
        Collection<? extends GrantedAuthority> authorities) {

    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(","));
}
```

It can then be stored in the JWT:

```java
.claim("authorities", authorities)
```

MERN comparison:

```javascript
const token = jwt.sign(
  {
    email: user.email,
    authorities: user.roles.join(",")
  },
  process.env.JWT_SECRET
);
```

The current Molla POS project normally uses one role, so it stores one `role` value instead of a comma-separated list.

# 46. Tutorial `JwtProvider` vs current `JwtUtil`

The class name changed, but the main idea is the same.

Tutorial structure:

```text
JwtProvider  → creates the token
JwtValidator → reads and validates the token
```

Current Molla POS structure:

```text
JwtUtil   → creates, parses and validates the token
JwtFilter → reads the request and establishes authentication
```

The current project creates a token with:

```java
jwtUtil.generateToken(user.getEmail(), user.getRole().name());
```

It stores:

```text
subject = email
role    = user's role
expiry  = 24 hours
```

Therefore:

> The tutorial's `JwtProvider` responsibility is mainly inside the final project's `JwtUtil`.

# 47. Login flow: where token generation happens

```text
User sends email and password
             ↓
AuthenticationManager checks them
             ↓
Authentication succeeds
             ↓
JwtProvider or JwtUtil creates a JWT
             ↓
Backend returns the JWT
             ↓
Frontend sends it with later requests
```

The current project checks credentials like this:

```java
authManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.getEmail(),
        request.getPassword()
    )
);
```

If authentication succeeds, it generates the token:

```java
String token = jwtUtil.generateToken(
    user.getEmail(),
    user.getRole().name()
);
```

MERN version:

```javascript
const user = await User.findOne({ email: req.body.email });

const matches = await bcrypt.compare(
  req.body.password,
  user.password
);

if (!matches) {
  return res.status(401).json({ message: "Invalid credentials" });
}

const token = jwt.sign(
  { role: user.role },
  process.env.JWT_SECRET,
  { subject: user.email, expiresIn: "24h" }
);

res.json({ token });
```

# 48. Signup flow

The application can also create a token immediately after registration:

```text
Validate signup data
        ↓
Hash the password
        ↓
Save the user
        ↓
Generate a JWT
        ↓
Return the token
```

This effectively logs the user in immediately after signup.

MERN version:

```javascript
const passwordHash = await bcrypt.hash(password, 10);

const user = await User.create({
  email,
  password: passwordHash,
  role
});

const token = jwt.sign(
  { role: user.role },
  process.env.JWT_SECRET,
  { subject: user.email, expiresIn: "24h" }
);
```

# 49. Authentication and authorization are different

Authentication asks:

> Who is this user?

Authorization asks:

> Is this user allowed to access this endpoint?

Easy connection:

```text
AuthenticationManager → checks login credentials
JwtProvider/JwtUtil    → creates the proof token
JwtFilter              → checks the proof on later requests
SecurityContextHolder  → keeps the authenticated user for this request
SecurityConfig         → checks whether that user has permission
```

MERN connection:

```text
bcrypt.compare()        → checks credentials
jwt.sign()              → creates the proof token
jwt.verify() middleware → checks the proof later
req.user                → keeps the current user
role middleware         → checks permission
```

# 50. Two uses of `UsernamePasswordAuthenticationToken`

The same class is used at two different times.

Before login is authenticated:

```java
new UsernamePasswordAuthenticationToken(email, password)
```

```text
principal   = email
credentials = password
authorities = not known yet
```

Inside the JWT filter, after the token is verified:

```java
new UsernamePasswordAuthenticationToken(
    userDetails,
    null,
    userDetails.getAuthorities()
)
```

```text
principal   = identified user
credentials = null
authorities = user's permissions
```

MERN comparison:

```javascript
// Login request contains credentials
const { email, password } = req.body;

// A later request gets the verified identity
req.user = {
  email: decoded.sub,
  role: decoded.role
};
```

# 51. The token creator and reader must agree

If the provider writes:

```java
.claim("email", email)
.claim("authorities", authorities)
```

the validator must read:

```java
claims.get("email")
claims.get("authorities")
```

The current project puts the email in the standard JWT subject:

```java
.subject(email)
```

Therefore it reads the email with:

```java
claims.getSubject()
```

If one class writes `role` but another tries to read `authorities`, it will not get the expected value.

MERN has the same rule:

```javascript
// Create
jwt.sign({ role: user.role }, secret);

// Read
const role = decoded.role;
```

# 52. Signing and verification must use the same secret

```text
JwtProvider/JwtUtil + secret key → signed JWT
JwtFilter/JwtUtil + same key     → valid or invalid
```

If the keys are different, verification fails. The secret should be kept in configuration or an environment variable, not hard-coded in source code.

MERN comparison:

```javascript
jwt.sign(payload, process.env.JWT_SECRET);
jwt.verify(token, process.env.JWT_SECRET);
```

Changing the secret also makes old tokens invalid unless key rotation is implemented.

# 53. Current token trade-off

The project uses a JWT that lasts for about 24 hours and does not appear to use a separate refresh-token flow.

- A long expiry is convenient.
- If the token is stolen, it may work until it expires.
- Removing it from the browser logs out that browser, but does not automatically invalidate the issued token on the server.

A larger production system may use short-lived access tokens, refresh tokens, refresh-token rotation and token revocation when immediate invalidation is needed. The same trade-off exists in MERN.

# 54. Useful project improvement to mention

The project currently contains authentication logic in the controller and similar older logic in `AuthServiceImp`.

A cleaner flow would be:

```text
AuthController
     ↓
AuthService
     ↓
AuthenticationManager + UserRepository + JwtUtil
```

Interview answer:

> In the learning version, some authentication logic is duplicated. I would refactor it into one authentication service so password validation, role rules and token generation have one source of truth.

# 55. Complete flow with `JwtProvider`

Login or signup:

```text
Client sends credentials
        ↓
AuthenticationManager checks credentials
        ↓
Authenticated user is available
        ↓
JwtProvider/JwtUtil creates and signs JWT
        ↓
Backend returns JWT
```

Every protected request after login:

```text
Client sends Authorization: Bearer <token>
        ↓
JwtFilter reads the token
        ↓
JwtUtil verifies signature and expiry
        ↓
Filter loads the user's current authorities
        ↓
Authentication is placed in SecurityContextHolder
        ↓
SecurityConfig checks the required role
        ↓
Controller runs if access is allowed
```

Interview-ready answer:

> In the tutorial, `JwtProvider` creates a signed JWT after login or registration succeeds. It takes the authenticated user's email and authorities and adds them as token claims. In my final Molla POS project, this responsibility is handled by `JwtUtil`, which also parses and validates tokens. On later requests, `JwtFilter` verifies the JWT, loads the current user authorities and places an authentication object in `SecurityContextHolder`. Spring Security then uses that authentication for authorization.

Final memory shortcut:

```text
AuthenticationManager = prove identity with credentials
JwtProvider/JwtUtil    = create the token
JwtFilter              = verify the token on later requests
SecurityContextHolder  = hold the current authenticated user
SecurityConfig         = decide whether access is allowed
```

# 58. `GrantedAuthority`

`GrantedAuthority` represents what an authenticated user is allowed to do.

```text
Authentication  = who is the user?
GrantedAuthority = what can the user do?
```

It is an interface with one important method:

```java
public interface GrantedAuthority extends Serializable {
    String getAuthority();
}
```

The returned string may be a role or a permission:

```text
ROLE_ADMIN
ROLE_STORE_ADMIN
READ_PRIVILEGE
```

## `SimpleGrantedAuthority`

Spring provides `SimpleGrantedAuthority` as the normal implementation:

```java
GrantedAuthority authority =
    new SimpleGrantedAuthority("ROLE_ADMIN");
```

In your project, `CustomUserImplementation` converts the user's database role into an authority:

```java
GrantedAuthority authority =
    new SimpleGrantedAuthority(user.getRole().toString());
```

That authority is placed inside the `UserDetails` object and later inside the authentication object.

## How authorities move through the JWT flow

### At login/token creation

Authorities can be converted into a string for a JWT claim:

```java
Collection<? extends GrantedAuthority> authorities =
    authentication.getAuthorities();

Set<String> authSet = new HashSet<>();

for (GrantedAuthority authority : authorities) {
    authSet.add(authority.getAuthority());
}

String rolesString = String.join(",", authSet);
```

The result could be:

```text
ROLE_ADMIN,ROLE_USER
```

### At request validation

The validator can convert the string back into authority objects:

```java
List<GrantedAuthority> authorities =
    AuthorityUtils.commaSeparatedStringToAuthorityList(
        "ROLE_ADMIN,ROLE_USER"
    );

Authentication auth =
    new UsernamePasswordAuthenticationToken(
        email,
        null,
        authorities
    );

SecurityContextHolder.getContext()
    .setAuthentication(auth);
```

Your current project instead loads current authorities from the database in `JwtFilter`, which avoids trusting only an old role claim.

## Roles and permissions

Roles normally use the `ROLE_` prefix:

```java
new SimpleGrantedAuthority("ROLE_ADMIN");
```

This matches:

```java
.hasRole("ADMIN")
```

Spring adds the `ROLE_` prefix for `hasRole`. Therefore, the authority is `ROLE_ADMIN`, while the configuration commonly writes `hasRole("ADMIN")`.

Specific permissions do not need the role prefix:

```java
new SimpleGrantedAuthority("READ_PRIVILEGE");
```

This matches:

```java
.hasAuthority("READ_PRIVILEGE")
```

## MERN comparison

MERN might store roles on the request like this:

```javascript
req.user = {
  roles: ["ADMIN", "USER"]
};
```

Then custom middleware checks:

```javascript
if (!req.user.roles.includes("ADMIN")) {
  return res.status(403).json({ message: "Forbidden" });
}
```

Spring uses standard authority objects inside `Authentication`, and the security configuration checks them:

```text
Express: req.user.roles.includes("ADMIN")
Spring:  hasRole("ADMIN") / hasAuthority("READ_PRIVILEGE")
```

## Interview answer

> `GrantedAuthority` represents a permission or role assigned to an authenticated user. `SimpleGrantedAuthority` wraps a string such as `ROLE_ADMIN`. During authentication, authorities are stored in `UserDetails` and then in the `Authentication` object. Spring Security uses them for rules such as `hasRole("ADMIN")` and `hasAuthority("READ_PRIVILEGE")`. This is similar to storing roles on `req.user` and checking them in Express middleware.

# 56. `UsernamePasswordAuthenticationToken` in the JWT filter

After the JWT has been verified and the user has been loaded, the filter creates an authentication object:

```java
Authentication authentication =
    new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

SecurityContextHolder.getContext()
    .setAuthentication(authentication);
```

This tells Spring Security who is making the current request and what that user is allowed to do.

## The three constructor arguments

### 1. `userDetails` — principal

This is the user loaded from the database. It implements `UserDetails` and represents the authenticated user.

Spring can later retrieve it with:

```java
Object principal = SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();
```

### 2. `null` — credentials

The second argument represents credentials, such as a password.

After a password has been checked or a JWT has been verified, the raw password or token is not needed in this authentication object. Passing `null` avoids keeping sensitive credentials there.

### 3. `userDetails.getAuthorities()` — permissions

This is the user's collection of roles or authorities, for example:

```text
ROLE_USER
ROLE_ADMIN
```

Spring uses these authorities for rules such as:

```java
.hasRole("ADMIN")
```

## Why are there two constructor forms?

Before credentials are checked, login code commonly creates:

```java
new UsernamePasswordAuthenticationToken(email, password)
```

This two-argument form represents authentication that still needs to be verified.

After the JWT is verified, the filter creates:

```java
new UsernamePasswordAuthenticationToken(
    userDetails,
    null,
    userDetails.getAuthorities()
)
```

The three-argument form represents the already-authenticated user and authorities. It is then placed in `SecurityContextHolder` for the current request.

## MERN comparison

Express middleware commonly does this:

```javascript
const decoded = jwt.verify(token, process.env.JWT_SECRET);

req.user = {
  email: decoded.sub,
  role: decoded.role
};

next();
```

Spring Security does the equivalent with a standard authentication object:

```java
Authentication auth = new UsernamePasswordAuthenticationToken(
    userDetails,
    null,
    userDetails.getAuthorities()
);

SecurityContextHolder.getContext().setAuthentication(auth);
```

Easy connection:

```text
Express: req.user
Spring:  SecurityContextHolder.getContext().getAuthentication()
```

## Interview answer

> In the JWT filter, `UsernamePasswordAuthenticationToken` represents the user whose token has already been verified. `userDetails` is the principal, `null` means no raw credentials need to be kept, and `getAuthorities()` supplies the user's roles. The authentication is stored in `SecurityContextHolder`, which is similar to attaching the verified user to `req.user` in Express.
