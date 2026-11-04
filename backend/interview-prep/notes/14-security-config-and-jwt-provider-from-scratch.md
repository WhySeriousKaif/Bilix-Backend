# Spring Security and JWT Provider — From the Beginning

This file explains the security setup in the order we would build it from an empty Spring Boot project.

## 1. Why does Spring print a generated password?

After adding:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Spring Security automatically protects the application. If we do not define our own security configuration, Spring creates:

```text
username = user
password = random password printed in the console
```

This default user is unrelated to MySQL. Spring Security does not automatically load users from our database. We must provide our own `UserDetailsService`, authentication configuration and security rules.

For a simple local test, fixed credentials can be configured:

```properties
spring.security.user.name=admin
spring.security.user.password=admin123
```

For the real API, we replace this default behavior with JWT security.

## 2. Create the security configuration class

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
}
```

```text
@Configuration      = this class contains Spring configuration
@EnableWebSecurity  = enable and customize web security
```

## 3. Create the password encoder bean

Passwords must not be stored as plain text:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

During signup:

```java
user.setPassword(passwordEncoder.encode(rawPassword));
```

During login, Spring compares the submitted password with the stored BCrypt hash.

## 4. Create the authentication manager bean

```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration)
        throws Exception {

    return configuration.getAuthenticationManager();
}
```

The `AuthenticationManager` is used during login to verify the email and password.

```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
```

## 5. Stateless session policy

JWT APIs normally do not use server-side login sessions:

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(
        SessionCreationPolicy.STATELESS
    )
)
```

This means:

```text
The server does not keep a JSESSIONID login session.
Every protected request must send its own Bearer token.
The JWT filter authenticates each request again.
```

## 6. URL authorization rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/api/super-admin/**")
        .hasRole("ADMIN")
    .requestMatchers("/api/**")
        .authenticated()
    .anyRequest().permitAll()
)
```

Rules are checked from top to bottom. More specific paths should come before general paths.

```text
permitAll()       = no login required
authenticated()   = valid authentication required
hasRole("ADMIN")  = authenticated user must have ROLE_ADMIN
```

## 7. Register the JWT filter

```java
.addFilterBefore(
    new JwtValidator(),
    BasicAuthenticationFilter.class
)
```

In the Molla POS project, the equivalent class is `JwtFilter`, and it is registered before Spring's username/password filter:

```java
http.addFilterBefore(
    jwtFilter,
    UsernamePasswordAuthenticationFilter.class
);
```

The filter:

```text
Reads Authorization: Bearer <token>
Removes Bearer
Verifies the JWT
Loads the user and authorities
Stores Authentication in SecurityContextHolder
```

## 8. Disable CSRF for this stateless API

```java
.csrf(AbstractHttpConfigurer::disable)
```

CSRF protection is mainly important for browser session/cookie authentication. This API sends a JWT in the Authorization header and does not use a server-side session.

## 9. Configure CORS

CORS allows the React frontend to call the backend from a different origin.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",
        "http://localhost:8000"
    ));

    config.setAllowCredentials(true);
    config.setAllowedMethods(Collections.singletonList("*"));
    config.setAllowedHeaders(Collections.singletonList("*"));
    config.setExposedHeaders(
        Collections.singletonList("Authorization")
    );
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Important distinction:

```text
setAllowedOrigins(...)    = which frontend origins are allowed
setAllowCredentials(true) = whether credentials may be included
```

`setAllowCredentials()` accepts a boolean, not a list of URLs.

Enable the source in the filter chain:

```java
.cors(cors -> cors.configurationSource(
    corsConfigurationSource()
))
```

## 10. Complete basic `SecurityConfig`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter) throws Exception {

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
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(
                corsConfigurationSource()
            ));

        return http.build();
    }
}
```

## 11. Create `JwtProvider`

`JwtProvider` creates the token after login succeeds.

```java
@Service
public class JwtProvider {

    private static final SecretKey key = Keys.hmacShaKeyFor(
        JwtConstant.JWT_SECRET.getBytes(StandardCharsets.UTF_8)
    );

    private static final long JWT_EXPIRATION_MS =
        24 * 60 * 60 * 1000L;

    public String generateToken(Authentication authentication) {

        String authorities = populateAuthorities(
            authentication.getAuthorities()
        );

        return Jwts.builder()
            .issuedAt(new Date())
            .expiration(new Date(
                System.currentTimeMillis()
                    + JWT_EXPIRATION_MS
            ))
            .claim("email", authentication.getName())
            .claim("authorities", authorities)
            .signWith(key)
            .compact();
    }

    private String populateAuthorities(
            Collection<? extends GrantedAuthority> authorities) {

        Set<String> values = new HashSet<>();

        for (GrantedAuthority authority : authorities) {
            values.add(authority.getAuthority());
        }

        return String.join(",", values);
    }
}
```

## 12. What each JWT builder method does

```java
.issuedAt(new Date())
```

Stores the creation time as the `iat` claim.

```java
.expiration(expiryDate)
```

Stores the expiry time as the `exp` claim.

```java
.claim("email", authentication.getName())
```

Stores the user's identity.

```java
.claim("authorities", authorities)
```

Stores roles such as `ROLE_ADMIN,ROLE_CASHIER`.

```java
.signWith(key)
```

Signs the header and payload so tampering can be detected.

```java
.compact()
```

Creates the final string:

```text
header.payload.signature
```

The JWT is signed, not encrypted. Its claims should not contain secrets.

## 13. Secret-key rules

```java
Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
```

The secret must be long enough for the selected HMAC algorithm. Keep it in environment configuration in production:

```properties
jwt.secret=${JWT_SECRET}
```

The same key must be used when creating and validating tokens. If the key changes, old tokens normally become invalid.

## 14. Provider and validator flow

```text
Login request
      ↓
AuthenticationManager verifies credentials
      ↓
JwtProvider.generateToken(authentication)
      ↓
Backend returns JWT
      ↓
Frontend stores JWT
      ↓
Frontend sends Authorization: Bearer <token>
      ↓
JwtFilter/JwtValidator reads and verifies token
      ↓
Extracts email and authorities
      ↓
Creates UsernamePasswordAuthenticationToken
      ↓
Stores it in SecurityContextHolder
      ↓
Authorization rules and controller execute
```

## 15. Current project naming note

The tutorial may call the token creator `JwtProvider` and the filter `JwtValidator`. The current Molla POS code uses:

```text
JwtUtil   = generates, parses and validates JWTs
JwtFilter = reads the request and establishes authentication
```

The responsibilities are the same even though the class names differ.

The current project generates tokens using:

```java
jwtUtil.generateToken(user.getEmail(), user.getRole().toString());
```

It stores the email as the standard subject (`sub`) and the role as a custom claim. The tutorial provider stores custom `email` and `authorities` claims. The creator and validator must agree on claim names.

## 16. Interview answer

> I first configure Spring Security to use stateless sessions, public authentication endpoints, protected API endpoints, a BCrypt password encoder and a custom JWT filter. After the AuthenticationManager verifies login credentials, the JWT provider creates a signed token containing the user's identity, authorities, issued time and expiry. The frontend sends that token as a Bearer token in later requests. The JWT filter verifies it, creates an authenticated `UsernamePasswordAuthenticationToken` and stores it in `SecurityContextHolder`, allowing Spring Security to apply authorization rules.

## 17. Final memory shortcut

```text
PasswordEncoder       = hash and compare passwords
AuthenticationManager = validate login credentials
SecurityFilterChain   = define security rules and filter order
JwtProvider/JwtUtil   = create JWT
JwtFilter/Validator   = verify JWT on requests
SecurityContextHolder = current request's authentication
CORS                  = allow the frontend origin
STATELESS             = no server-side login session
```
