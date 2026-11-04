# Project Mistakes and Learnings

This file records mistakes found while building and testing the project so they are not repeated.

## 1. Controller URL and security URL must match

The controller currently has:

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupHandler(...) {
        ...
    }
}
```

Therefore the real endpoint is:

```text
POST /auth/signup
```

It is not:

```text
POST /api/signup
```

The full URL must match the controller mapping:

```text
http://localhost:5001/auth/signup
```

## 2. Public authentication paths must be permitted

If authentication endpoints are under `/auth`, configure:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/api/super-admin/**")
        .hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)
```

If the controller instead uses `/api/auth`, then the rule must be:

```java
.requestMatchers("/api/auth/**").permitAll()
```

The matcher and controller path must use the same prefix.

## 3. Why `No Auth` works for signup/login

Signup and login are public because a user does not have a JWT before signing up or logging in.

```text
POST /auth/signup → no JWT required
POST /auth/login  → no JWT required
```

Protected endpoints need a header:

```http
Authorization: Bearer <jwt>
```

## 4. Difference between 404 and 401/403

```text
404 Not Found
    = URL does not match a controller mapping

401 Unauthorized
    = endpoint is protected but authentication is missing or invalid

403 Forbidden
    = user is authenticated but lacks the required authority
```

If Postman shows:

```text
No static resource api/signup
```

the usual problem is that `/api/signup` does not exist in any controller. Check the URL before debugging the password or JWT.

## 5. Common debugging checklist

```text
1. Confirm server port.
2. Read @RequestMapping on the controller.
3. Read @PostMapping or @GetMapping on the method.
4. Combine the two paths.
5. Compare that path with Postman.
6. Check the Spring Security request matcher.
7. Decide whether the endpoint should be public.
8. Add Authorization only when the endpoint is protected.
```

## 6. Security configuration lesson

This rule:

```java
.requestMatchers("/api/**").authenticated()
.anyRequest().permitAll()
```

protects `/api/**`, while paths outside `/api` are allowed by the final rule.

If you change the final rule to:

```java
.anyRequest().authenticated()
```

then `/auth/signup` and `/auth/login` must be explicitly permitted first:

```java
.requestMatchers("/auth/**").permitAll()
.anyRequest().authenticated()
```

Otherwise signup and login will require a JWT, which is logically impossible for a new user.

## 7. Final memory shortcut

```text
Controller mapping + method mapping = actual endpoint URL
Security matcher must match the same URL
Signup/login = public
Business APIs = protected
404 = wrong route
401 = missing/invalid authentication
403 = insufficient permission
```
