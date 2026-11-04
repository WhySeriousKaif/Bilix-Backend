# Exception Handling in Spring Boot — Molla POS

## 1. What is an exception?

An exception means something unexpected happened while the application was running.

Examples:

```text
User does not exist
Invalid request data
Duplicate email or SKU
Missing branch ID
Database constraint failure
```

Instead of returning a successful response, the application must return a useful HTTP error response.

## 2. Exceptions created in this project

The project has these custom exceptions:

```text
com.molla.exceptions.UserException
com.molla.exceptions.BadRequestException
com.molla.exceptions.NotFoundException
com.molla.exceptions.GlobalExceptionHandler
```

`GlobalExceptionHandler` is not an exception. It is the central class that catches exceptions and converts them into JSON responses.

## 3. `UserException`

```java
public class UserException extends Exception {

    public UserException(String message) {
        super(message);
    }
}
```

This is a **checked exception** because it extends `Exception` directly.

If a method throws it, Java requires the method to either catch it or declare it with `throws`:

```java
public User getUserById(Long id) throws UserException {
    // method code
}
```

The controller or caller may also declare it:

```java
public ResponseEntity<UserDto> getUserById(
        @PathVariable Long id) throws UserException {

    User user = userService.getUserById(id);
    return ResponseEntity.ok(UserMapper.toDto(user));
}
```

In the current project, `UserException` is handled as a `400 BAD_REQUEST` by the global handler.

## 4. `BadRequestException`

```java
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
```

This represents an invalid request or business rule violation.

Examples from the project:

```java
throw new BadRequestException(
    "User already exists with email: " + email
);
```

```java
throw new BadRequestException(
    "You need to create a store before adding products."
);
```

Because it extends `RuntimeException`, the method does not have to write `throws BadRequestException`.

Typical response:

```text
HTTP 400 Bad Request
```

## 5. `NotFoundException`

```java
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

This represents a resource that does not exist.

Example from `UserServiceImp`:

```java
User user = userRepository.findByEmail(email);

if (user == null) {
    throw new NotFoundException("User not found");
}
```

Another common form uses `Optional.orElseThrow()`:

```java
return userRepository.findById(id)
    .orElseThrow(() ->
        new NotFoundException("User not found with id: " + id)
    );
```

Typical response:

```text
HTTP 404 Not Found
```

## 6. Understanding `orElseThrow()`

This code:

```java
return userRepository.findById(id)
    .orElseThrow(() -> new UserException("User not found"));
```

means:

```text
1. Search for the user by ID.
2. If the user exists, return it.
3. If the Optional is empty, create and throw UserException.
```

The lambda is only used when no value is found:

```java
() -> new UserException("User not found")
```

## 7. The global exception handler — the glue

The project uses:

```java
@RestControllerAdvice(basePackages = "com.molla.controllers")
public class GlobalExceptionHandler {
}
```

`@RestControllerAdvice` tells Spring:

> Watch exceptions thrown by controller requests and handle them centrally.

The handler connects exception classes to HTTP responses with `@ExceptionHandler`:

```java
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<Map<String, Object>> handleNotFound(
        NotFoundException ex) {

    return build(
        HttpStatus.NOT_FOUND,
        "NOT_FOUND",
        ex.getMessage()
    );
}
```

Another example:

```java
@ExceptionHandler(BadRequestException.class)
public ResponseEntity<Map<String, Object>> handleBadRequest(
        BadRequestException ex) {

    return build(
        HttpStatus.BAD_REQUEST,
        "BAD_REQUEST",
        ex.getMessage()
    );
}
```

## 8. How an exception travels through the project

```text
Controller calls service
        ↓
Service cannot complete the operation
        ↓
Service throws an exception
        ↓
Exception travels back through the controller
        ↓
GlobalExceptionHandler catches it
        ↓
Handler creates an HTTP error response
        ↓
Frontend receives consistent JSON
```

Example:

```text
GET /api/users/999
        ↓
userRepository.findById(999) is empty
        ↓
NotFoundException is thrown
        ↓
handleNotFound() runs
        ↓
HTTP 404 response is returned
```

## 9. Common response format

The handler's `build()` method creates one common structure:

```java
private ResponseEntity<Map<String, Object>> build(
        HttpStatus status,
        String error,
        String message) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("error", error);
    body.put("message", message);
    body.put("status", status.value());

    return ResponseEntity.status(status).body(body);
}
```

The frontend receives a predictable response:

```json
{
  "timestamp": "2026-10-26T12:00:00Z",
  "error": "NOT_FOUND",
  "message": "User not found with id: 999",
  "status": 404
}
```

## 10. Other handlers in this project

### Validation errors

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
```

Handles validation annotations such as:

```java
@NotBlank
@Email
```

### Database constraint errors

```java
@ExceptionHandler(DataIntegrityViolationException.class)
```

Handles database problems such as duplicate or missing required values.

### Fallback handler

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        ex.getMessage()
    );
}
```

This catches unexpected exceptions and returns HTTP 500.

Specific handlers should be used before the general `Exception` handler so that the application can return the correct status and message.

## 11. Checked vs unchecked exceptions

```text
UserException
    extends Exception
    checked exception
    caller must catch or declare with throws

BadRequestException
    extends RuntimeException
    unchecked exception
    no mandatory throws declaration

NotFoundException
    extends RuntimeException
    unchecked exception
    no mandatory throws declaration
```

The `throws` keyword does not throw the exception by itself. It only tells Java that the method may pass that exception to its caller.

`throw` actually creates and sends the exception:

```java
throw new NotFoundException("User not found");
```

## 12. MERN comparison

Spring Boot:

```java
throw new NotFoundException("User not found");
```

MERN:

```javascript
return res.status(404).json({
  error: "NOT_FOUND",
  message: "User not found"
});
```

Spring's `GlobalExceptionHandler` is similar to centralized Express error middleware:

```javascript
app.use((err, req, res, next) => {
  res.status(err.statusCode || 500).json({
    error: err.code || "INTERNAL_ERROR",
    message: err.message
  });
});
```

## 13. Interview answer

> In my Spring Boot project, services throw meaningful exceptions such as `NotFoundException` for missing resources and `BadRequestException` for invalid requests or business rule violations. `UserException` is a checked exception, so methods that use it declare `throws UserException`. `GlobalExceptionHandler`, marked with `@RestControllerAdvice`, catches these exceptions centrally and converts them into consistent JSON responses with the correct HTTP status. This keeps controllers clean and gives the frontend one predictable error format.

## 14. Final memory shortcut

```text
throw                  = actually raise an exception
throws                 = declare that a method may raise it
NotFoundException      = resource does not exist → 404
BadRequestException    = invalid request/rule → 400
UserException          = checked project exception
@RestControllerAdvice  = global exception handling
@ExceptionHandler      = maps exception to response
Exception.class        = final fallback → 500
```

## 15. Where to add `@RestControllerAdvice`

Create a separate handler class inside the project’s `exceptions` package:

```text
src/main/java/com/bilix/PosSystem/exceptions/GlobalExceptionHandler.java
```

```java
package com.bilix.PosSystem.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, String>> handleUserException(
            UserException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}
```

### What happens during a request?

```text
Controller/Service throws UserException
        ↓
Spring finds GlobalExceptionHandler
        ↓
@ExceptionHandler(UserException.class) runs
        ↓
Client receives a small JSON response instead of a stack trace
```

For example, this service code:

```java
return userRepository.findById(id)
        .orElseThrow(() -> new UserException("User not found!"));
```

returns:

```json
{
  "message": "User not found!"
}
```

The handler class must be inside, or below, the main application package (`com.bilix.PosSystem`) so Spring can discover it automatically. `@RestControllerAdvice` combines global `@ControllerAdvice` behavior with `@ResponseBody`, so the returned object becomes JSON.

### Choosing the status code

Use `404 NOT_FOUND` when a requested user or resource does not exist. For duplicate registration, use `409 CONFLICT`; for invalid input, use `400 BAD_REQUEST`. Add separate handler methods when different exceptions need different status codes.

### MERN comparison

`@RestControllerAdvice` is similar to Express’s final error middleware:

```javascript
app.use((err, req, res, next) => {
  res.status(err.statusCode || 500).json({
    message: err.message
  });
});
```

Interview line:

> I keep exception handling centralized with `@RestControllerAdvice`. Individual services throw meaningful exceptions, and the global handler converts them into consistent JSON responses with suitable HTTP status codes.
