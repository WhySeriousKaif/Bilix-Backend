# Payload, DTO, Request and Response — Easy Revision Notes

## 1. What is the `payload` package?

The `payload` package contains objects that travel between the frontend and backend.

```text
payload/dto       = data coming into the backend
payload/response  = data going out to the frontend
```

## 2. Request DTO

For example, `LoginRequest` receives login data:

```java
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
```

The frontend sends JSON:

```json
{
  "email": "kaif@example.com",
  "password": "123456"
}
```

Spring converts this JSON into a `LoginRequest` object. The validation annotations check that the input is valid.

## 3. Response class

`AuthResponse` represents the data returned after login:

```java
public class AuthResponse {
    private String jwt;
    private String message;
    private UserDto user;
}
```

The backend can return:

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Login successful",
  "user": {
    "id": 1,
    "name": "Kaif"
  }
}
```

## 4. `ApiResponse`

`ApiResponse` is useful when the backend only needs to return a standard message:

```java
public class ApiResponse {
    private String message;
}
```

Example:

```json
{
  "message": "User deleted successfully"
}
```

## 5. Why not return the Entity directly?

Entities represent database data. DTOs represent API data.

We use DTOs because:

- An entity may contain sensitive fields such as a password.
- The database structure should not be exposed directly.
- The request format and response format may be different.
- Validation is easier to keep in request DTOs.
- Responses stay consistent for the frontend.

## 6. MERN comparison

```text
Express req.body       = Spring request DTO
res.json({...})        = Spring response DTO
Mongoose document      = Spring entity
```

## 7. Complete flow

```text
Frontend JSON request
        ↓
Request DTO
        ↓
Controller
        ↓
Service + Entity
        ↓
Response DTO
        ↓
JSON response to frontend
```

## 8. Interview answer

> The payload package acts as the API boundary. Request DTOs receive and validate client data, while response classes define the data sent back to the frontend. We do not return entities directly because entities are tied to the database and may expose sensitive or unnecessary fields. DTOs keep the API contract separate from the database model.

## 9. Final memory shortcut

```text
DTO       = safe data shape for the API
Request   = data coming into the backend
Response  = data going back to the frontend
Entity    = database model
Payload   = data travelling through the API
```

## 10. Entity-to-DTO mapping in this project

The project uses mapper classes to convert database entities into API DTOs.

```text
User entity → UserMapper → UserDto → API response
```

For example, `UserMapper.toDto(user)` copies safe fields such as the ID, name, email, role and related branch/store IDs. It does not expose the user's password in the response.

This keeps the database model separate from the data sent to the frontend.

```java
UserDto userDto = new UserDto();
userDto.setId(user.getId());
userDto.setFullName(user.getFullName());
userDto.setEmail(user.getEmail());
userDto.setRole(user.getRole());
```

The reverse direction is used when incoming data must become an entity:

```text
Request DTO → Mapper → Entity → save to database
```

For passwords, the project should encode the password before saving it. A password should never be copied into a normal response DTO.

## 11. `ResponseEntity`

`ResponseEntity` is Spring's wrapper for the complete HTTP response:

```text
HTTP status code + headers + response body
```

### The project style

```java
@GetMapping("/profile")
public ResponseEntity<UserDto> getUserProfile(
        @RequestHeader("Authorization") String jwt) {

    User user = userService.getUserFromJwt(jwt);
    return ResponseEntity.ok(UserMapper.toDto(user));
}
```

`ResponseEntity.ok(...)` means:

```text
status = 200 OK
body   = mapped UserDto
```

### Returning a raw object

```java
@GetMapping("/profile")
public UserDto getUserProfile() {
    return UserMapper.toDto(user);
}
```

Spring normally serializes the object as JSON and uses `200 OK`. This is shorter, but `ResponseEntity` gives the controller explicit control over status codes and headers.

### Equivalent forms

```java
return ResponseEntity.ok(dto);
```

is equivalent to:

```java
return new ResponseEntity<>(dto, HttpStatus.OK);
```

or:

```java
return ResponseEntity.status(HttpStatus.OK).body(dto);
```

### Common shortcuts

```java
ResponseEntity.ok(dto);                         // 200 + body
ResponseEntity.created(uri).body(dto);          // 201 + body
ResponseEntity.noContent().build();             // 204, no body
ResponseEntity.notFound().build();              // 404, no body
ResponseEntity.badRequest().body(error);        // 400 + error body
```

The full constructor equivalents are:

```java
new ResponseEntity<>(dto, HttpStatus.OK);
new ResponseEntity<>(dto, HttpStatus.CREATED);
new ResponseEntity<>(HttpStatus.NO_CONTENT);
new ResponseEntity<>(HttpStatus.NOT_FOUND);
new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
```

### MERN comparison

```text
Express: res.status(200).json(dto)
Spring:  ResponseEntity.ok(dto)
```

```text
Express: res.status(201).json(newItem)
Spring:  ResponseEntity.status(HttpStatus.CREATED).body(newItem)
```

```text
Express: res.sendStatus(204)
Spring:  ResponseEntity.noContent().build()
```

### Interview answer

> `ResponseEntity` represents the complete HTTP response, including the status, headers and body. In this project, `ResponseEntity.ok(UserMapper.toDto(user))` returns the mapped DTO with HTTP 200. We use other builder methods when we need a different response, such as 201 after creation, 204 after deletion, or 400/404 for errors.
