# Part 2: Authorization and Access Control

This is an important part of your POS backend. Authentication and authorization are two different things:

- Authentication: Who are you? (Login using email and password, then receive a JWT.)
- Authorization: What are you allowed to access? (Can this cashier access Branch A? Can this manager delete an employee?)

Your code is implementing role-based access control (RBAC) combined with resource-level access control.

The second part is especially important in a POS application because multiple stores and branches can exist in the same database.

Let's understand your implementation, then I'll point out the changes I'd make before calling it production-ready.

## 1. AccessControlService — the security guard

This is arguably the most important class in the code you shared.

Imagine your POS application has:

- Store A
  - Branch A1
  - Branch A2
- Store B
  - Branch B1

Suppose a cashier works at Branch A1.

Even if that cashier knows the ID of Branch B1, they must not be able to access its information.

That's exactly what `AccessControlService` is designed to prevent.

### Why did you create a separate service?

Without this class, you might repeat access checks in multiple places:

```
if (user.getRole() == UserRole.ROLE_ADMIN) {
    // Allow access
} else {
    // Check branch ownership
}
```

You might write that in:

- BranchController
- EmployeeController
- OrderService
- InventoryService
- RefundService

This creates duplicated security logic.

Instead, you have one central class:

```
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final BranchRepository branchRepository;
}
```

- `@Service`: Spring registers this class as a service bean.
- `@RequiredArgsConstructor`: Lombok generates a constructor for final fields.
- `BranchRepository`: Used to retrieve a branch and check which store owns it.

Other services can inject `AccessControlService` and reuse its methods.

## 2. Understanding each access-control method

### A. `requireBranchAccess()`

```
public void requireBranchAccess(User user, Long branchId) {
    if (branchId == null) {
        throw new BadRequestException("Branch id is required");
    }

    if (user.getRole() == UserRole.ROLE_ADMIN) {
        return;
    }

    Branch branch = branchRepository.findById(branchId)
            .orElseThrow(() ->
                new NotFoundException("Branch not found with id: " + branchId));
```

First, it checks whether a branch ID was provided.

Then it checks whether the user is a super admin.

If the user is an admin, it immediately returns.

Why?

Because the admin has system-wide access.

Otherwise, it fetches the requested branch from the database.

Now comes the interesting part:

```
if (user.getRole() == UserRole.ROLE_STORE_ADMIN
        || user.getRole() == UserRole.ROLE_STORE_MANAGER) {

    Long userStoreId = user.getStore() != null
            ? user.getStore().getId() : null;

    Long branchStoreId = branch.getStore() != null
            ? branch.getStore().getId() : null;

    if (userStoreId != null && userStoreId.equals(branchStoreId)) {
        return;
    }

    throw new BadRequestException(
            "You do not have access to this branch");
}
```

A Store Admin or Store Manager can access branches belonging to their own store.

For example:

Store A

Store Manager's store ID

101

Requested branch's store ID

101

Access allowed

Store B

Store Manager's store ID

101

Requested branch's store ID

202

Access denied

Finally, for branch-level staff:

```
Long userBranchId = user.getBranch() != null
        ? user.getBranch().getId() : null;

if (userBranchId != null && userBranchId.equals(branchId)) {
    return;
}
```

A Branch Manager or cashier can only access their assigned branch.

Important concept: This is resource-level authorization. Checking someone's role alone isn't enough; you also check which resource they own or are assigned to.

### B. `requireStoreAccess()`

This method performs a similar check, but at the store level.

The interesting part is:

```
Long userStoreId = user.getStore() != null
        ? user.getStore().getId()
        : (user.getBranch() != null
                && user.getBranch().getStore() != null
                ? user.getBranch().getStore().getId()
                : null);
```

This is a nested ternary operator.

It means:

1. If the user has a store directly assigned, use that store ID.
2. Otherwise, if the user has a branch assigned, get the store associated with that branch.
3. Otherwise, use `null`.

Why is this necessary?

Because a cashier might have a branch assigned but might not have a direct `store` relationship populated on their User entity.

The branch itself knows which store it belongs to.

### C. `requireManagerAccessToBranch()`

This method is stricter than `requireBranchAccess()`.

Consider:

| Role           | Access to branch      |
| -------------- | --------------------- |
| Super Admin    | All branches          |
| Store Admin    | Branches in own store |
| Store Manager  | Branches in own store |
| Branch Manager | Their assigned branch |
| Cashier        | Denied                |
| Branch Cashier | Denied                |

Why?

Because viewing branch information and managing employees are different permissions.

A cashier might need to view branch details, but shouldn't be able to create or delete employees.

This is a good example of separating permissions by business operation.

### D. `requireBranchInfoEditAccess()`

This method allows:

- Super Admin: All branches.
- Store Admin/Store Manager: Branches belonging to their store.
- Other users: Their assigned branch.

Your comment says even cashiers can edit their branch's information.

However, there is an important issue here: the code doesn't explicitly restrict the remaining roles to staff roles.

Any non-admin, non-store-manager user with a matching branch ID could pass this check, including `ROLE_USER`, if such a user has a branch assigned.

I'd explicitly allow only the intended staff roles.

### E. `requireManagerAccessToStore()`

This is the store-level equivalent of manager access.

Only these roles are allowed:

- `ROLE_ADMIN`
- `ROLE_STORE_ADMIN`
- `ROLE_STORE_MANAGER`

A branch manager or cashier cannot manage the entire store.

### F. `requireAdmin()`

```
public void requireAdmin(User user) {
    if (user.getRole() != UserRole.ROLE_ADMIN) {
        throw new BadRequestException(
                "Only a Super Admin can perform this action");
    }
}
```

This is a straightforward role check.

For example, system-wide reports, platform configuration, or administrative actions might require this permission.

### G. `requireSelfOrAdmin()`

```
public void requireSelfOrAdmin(User user, Long targetUserId) {
    if (user.getRole() == UserRole.ROLE_ADMIN) {
        return;
    }

    if (user.getId() != null && user.getId().equals(targetUserId)) {
        return;
    }

    throw new BadRequestException(
            "You do not have access to this resource");
}
```

This prevents a user from accessing another user's personal resources.

Example:

- User ID 10 requests their own shift: allowed.
- User ID 10 requests User ID 20's shift: denied.
- Super Admin requests User ID 20's shift: allowed.

This is also called an ownership check.

## 3. UserService — the business logic layer

Your interface:

```
public interface UserService {

    User getUserFromJwt(String jwt);

    User getCurrentUser();

    User getUserByEmail(String email);

    User getUserById(Long id);

    List<User> getAllUsers();

    Page<User> getUsersPaged(
        int page,
        int size,
        String sortBy,
        String direction,
        String search,
        UserRole role
    );

    User updateUser(User user);
}
```

This is an interface.

It defines what operations are available, but it doesn't define how those operations work.

Your `UserServiceImp` implements these methods.

Why use an interface?

Because your controller depends on the abstraction:

```
private final UserService userService;
```

It doesn't need to know whether the implementation is `UserServiceImp` or another implementation.

This makes testing and future changes easier.

## 4. UserServiceImp — understanding the implementation

### A. `getUserFromJwt()`

```
public User getUserFromJwt(String jwt) {

    String cleanToken = jwt != null ? jwt.trim() : "";

    if (cleanToken.startsWith("Bearer ")) {
        cleanToken = cleanToken.substring(7).trim();
    }

    if (cleanToken.isEmpty()) {
        throw new NotFoundException("JWT token is required");
    }

    String email = jwtUtil.extractEmail(cleanToken);

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new NotFoundException("User not found");
    }

    return user;
}
```

Let's understand why this exists.

Suppose the frontend sends this HTTP header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The word `Bearer` is not part of the JWT itself.

So your code removes it:

```
cleanToken = cleanToken.substring(7).trim();
```

Why 7?

Because `"Bearer "` contains seven characters.

Then:

```
String email = jwtUtil.extractEmail(cleanToken);
```

Your `JwtUtil` reads the token's subject (`sub`), which contains the user's email.

Then you query the database:

```
User user = userRepository.findByEmail(email);
```

Finally, you return the actual application `User` entity.

Behind the scenes:

```
Authorization Header
        |
        v
Remove "Bearer "
        |
        v
Validate and parse JWT
        |
        v
Extract email from subject
        |
        v
Find user in database
        |
        v
Return User entity
```

One important detail: your `extractEmail()` method uses JWT parsing, which verifies the signature and checks token expiration in the JJWT parser you showed earlier. So this is not simply decoding an arbitrary token.

However, in your application, this method may be unnecessary if your `JwtFilter` already authenticates the request and puts the user identity into Spring Security's `SecurityContextHolder`.

I'll explain that distinction below.

### B. `getCurrentUser()`

```
public User getCurrentUser() {

    String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new NotFoundException("User not found");
    }

    return user;
}
```

This is a very important Spring Security concept.

Remember that after a valid JWT is received, your JWT filter should:

1. Extract the token.
2. Validate it.
3. Extract the email.
4. Create an authenticated `Authentication` object.
5. Store that object in `SecurityContextHolder`.

For example:

```
SecurityContextHolder.getContext()
        .setAuthentication(authentication);
```

Now any service or controller can retrieve the authenticated user's identity.

Your code:

```
SecurityContextHolder.getContext()
        .getAuthentication()
        .getName();
```

Gets the authenticated principal's name.

In your implementation, that name is the email.

Then you fetch the complete User entity from the database.

Real-world example:

Kaif logs in with `kaif@gmail.com`.

The JWT filter validates his token and sets the authentication name to `kaif@gmail.com`.

When he calls:

```
GET /api/users/profile
```

Your `getCurrentUser()` retrieves his email from the security context and fetches his User record.

The frontend doesn't need to send a user ID to identify whose profile it wants.

That's exactly why this approach is useful.

### C. `getUserByEmail()` and `getUserById()`

These are straightforward database lookup methods.

The interesting annotation is:

```
@Cacheable(cacheNames = "users", key = "#id")
```

It means Spring checks the cache before querying the database.

For example:

First request:

```
getUserById(10)
     |
     v
Cache miss
     |
     v
Database query
     |
     v
Save result in cache
```

Second request:

```
getUserById(10)
     |
     v
Cache hit
     |
     v
Return cached result
```

This can reduce repeated database queries.

Your `@CacheEvict` on `updateUser()` clears the relevant caches after an update.

### D. `getAllUsers()`

```
@Cacheable(cacheNames = "usersAll")
public List<User> getAllUsers() {
    return userRepository.findAll();
}
```

This fetches every user in the database and caches the result.

The caching itself is fine, but there's a major authorization concern here, which I'll cover shortly.

### E. `getUsersPaged()`

This method handles pagination, sorting and filtering.

```
page = Math.max(page, 0);
size = size <= 0 || size > 100 ? 10 : size;
```

Why?

Because a client might send:

```
GET /api/users/paged?page=-10&size=100000
```

Your code ensures:

- Page cannot be negative.
- Page size must be between 1 and 100.
- Invalid page sizes default to 10.

Next:

```
Set<String> allowedSorts = Set.of(
    "id", "fullName", "email", "role", "createdAt"
);
```

This is an allowlist.

The client cannot freely choose any database property for sorting.

```
sortBy = allowedSorts.contains(sortBy)
        ? sortBy
        : "createdAt";
```

If the client requests an unsupported field, your code falls back to `createdAt`.

Then:

```
Sort sort = "asc".equalsIgnoreCase(direction)
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();
```

This creates either ascending or descending sorting.

Finally:

```
return userRepository.findAllFiltered(
    normalizedSearch,
    role,
    PageRequest.of(page, size, sort)
);
```

`PageRequest.of()` creates a Spring Data pagination request.

For example:

```
GET /api/users/paged?page=0&size=10&sortBy=fullName&direction=asc
```

It requests the first 10 users, sorted by name in ascending order.

`Page<User>` contains more than just the returned records:

- Content (the users on this page).
- Total number of records.
- Total pages.
- Current page number.
- Whether another page exists.

That's why `Page` is useful for frontend pagination.

### F. `updateUser()`

```
@CacheEvict(
    cacheNames = {"users", "usersAll"},
    allEntries = true
)
public User updateUser(User user) {
    return userRepository.save(user);
}
```

This saves the user and clears both caches.

`allEntries = true` means all entries in those named caches are removed.

One concern: accepting an entire `User` entity and saving it directly can be dangerous if the entity was built from client input. A client might accidentally or deliberately modify fields such as `role`, `store`, or `branch`.

Prefer accepting a dedicated update DTO, fetching the existing user, checking permissions, and updating only allowed fields.

## 5. UserController — the API layer

Your controller exposes the service methods as HTTP endpoints.

```
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
}
```

- `@RestController`: Makes this a REST API controller.
- `@RequestMapping("/api/users")`: Sets the common URL prefix.
- `@RequiredArgsConstructor`: Generates the constructor for `userService`.

### Your endpoints

| Endpoint                 | Purpose                                |
| ------------------------ | -------------------------------------- |
| `GET /api/users/profile` | Get currently logged-in user's profile |
| `GET /api/users/{id}`    | Get a user by ID                       |
| `GET /api/users/all`     | Get all users                          |
| `GET /api/users/paged`   | Get paginated and filtered users       |

For example:

```
@GetMapping("/profile")
public ResponseEntity<UserDto> getUserProfile() {
    return ResponseEntity.ok(
        UserMapper.toDto(userService.getCurrentUser())
    );
}
```

The flow is:

```
Frontend
   |
   | GET /api/users/profile
   | Authorization: Bearer <JWT>
   v
Security Filter / JwtFilter
   |
   v
UserController
   |
   v
UserService
   |
   v
SecurityContextHolder
   |
   v
UserRepository
   |
   v
Database
   |
   v
UserMapper
   |
   v
UserDto
   |
   v
Frontend
```

### Why do you use UserMapper?

Your database entity may contain sensitive information:

```
User {
    id;
    fullName;
    email;
    password;
    role;
    store;
    branch;
}
```

You should not return the entity directly to the frontend.

Instead:

```
UserMapper.toDto(user)
```

Converts the entity into a DTO containing only the information the frontend is supposed to receive.

This is a good practice.

## 6. UserRole — understanding your enum

Your enum:

```
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

This defines the roles available in your application.

You also have:

```
@JsonCreator
public static UserRole fromString(String value)
```

`@JsonCreator` tells Jackson to use this method when converting incoming JSON strings into `UserRole` values.

For example, the frontend might send:

```
{
    "role": "CASHIER"
}
```

Your method normalizes the value and converts it to:

```
UserRole.ROLE_CASHIER
```

This makes your API more flexible.

One small improvement:

```
String normalized = value.toUpperCase(Locale.ROOT)
        .replace(" ", "_");
```

Using `Locale.ROOT` avoids locale-dependent uppercase conversion.

# 7. Important code review: What should you fix?

Your overall architecture is good. However, I noticed some security issues that I would fix before deploying this application.



Important security issue

&#x20;1\. Your UserController doesn't show authorization checks



Look at this:

```
@GetMapping("/{id}")
public ResponseEntity<UserDto> getUserById(
        @PathVariable("id") Long id) {

    return ResponseEntity.ok(
        UserMapper.toDto(userService.getUserById(id))
    );
}
```

What happens if a cashier from Branch A requests another employee's user ID from Branch B?

Your controller calls `getUserById()` without checking whether the caller has permission to access that user.

Similarly:

```
@GetMapping("/all")
```

and:

```
@GetMapping("/paged")
```

currently return users without any store or branch restrictions in the code shown.

Having an `AccessControlService` doesn't automatically secure your application. You must actually call its methods before accessing protected resources.

For example, you could inject it into the relevant service and perform the authorization check before returning the data.

```
public User getUserById(Long id) {

    User currentUser = getCurrentUser();

    User targetUser = userRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("User not found"));

    accessControlService.requireSelfOrAdmin(
            currentUser,
            targetUser.getId()
    );

    return targetUser;
}
```

This is only an example of a self-or-admin policy. Your actual user lookup policy may need to allow store managers to view their employees and branch managers to view their branch staff.

2\. Protect your `getCurrentUser()` method against missing authentication

Currently:

```
String email = SecurityContextHolder.getContext()
        .getAuthentication()
        .getName();
```

If authentication is missing, this can throw a `NullPointerException`.

Add checks for missing authentication and anonymous authentication, and return an appropriate unauthorized response.

3\. Protect against null users and roles

Your `AccessControlService` repeatedly uses:

```
user.getRole()
```

If `user` or `user.getRole()` is null, the code can throw a `NullPointerException`.

Validate the user and role at the beginning of your access-control methods.

4\. Restrict branch information editing to intended staff roles

Your `requireBranchInfoEditAccess()` allows any non-store-level user with the matching branch ID.

Explicitly allow only the roles you intend, such as:

- `ROLE_BRANCH_MANAGER`
- `ROLE_CASHIER`
- `ROLE_BRANCH_CASHIER`
- `ROLE_STORE_EMPLOYEE` (if appropriate)

5\. Be careful with caching user entities

You are caching complete `User` entities.

For a production application, consider caching DTOs or carefully configuring entity serialization and cache invalidation. Also ensure that updates, deletions, role changes and store/branch reassignment invalidate affected cache entries.

6\. Make database operations transactional

For operations involving multiple related database writes, use `@Transactional` on the service method. This is particularly useful when creating users and assigning them to branches or stores.

## 8. One important distinction: Authentication vs Authorization

Let's connect everything.

## Complete request security flow

1\. User logs in

AuthenticationManager verifies credentials

2\. JwtUtil generates JWT

Token contains user identity and role

3\. Frontend sends JWT

Authorization: Bearer token

4\. JwtFilter validates token

Sets Authentication in SecurityContextHolder

5\. UserService gets current user

Identifies the logged-in user

6\. AccessControlService checks permissions

Checks role, store ownership and branch assignment

7\. Operation executes

Only if the user is authorized

That's the complete idea behind your security architecture.

## 9. Interview questions you should know

| Question                                                         | Short answer                                                                         |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| What is authorization?                                           | Determining what an authenticated user can access or perform.                        |
| What is RBAC?                                                    | Role-Based Access Control; permissions depend on assigned roles.                     |
| What is resource-level authorization?                            | Checking whether a user can access a particular resource, such as a branch or store. |
| Why create AccessControlService?                                 | To centralize and reuse authorization logic.                                         |
| What is SecurityContextHolder?                                   | Spring Security's holder for the current request's security context.                 |
| What is the difference between Authentication and Authorization? | Authentication verifies identity; authorization checks permissions.                  |
| Why use DTOs?                                                    | To control which data is exposed through APIs.                                       |
| Why use Page instead of List?                                    | To support pagination and avoid loading all records at once.                         |

## 10. My recommendation for your project

I would keep your overall class structure:

```
service/
    AccessControlService.java
    UserService.java
    impl/
        UserServiceImp.java

controllers/
    UserController.java

domain/
    UserRole.java
```

You don't need to rewrite this architecture.

But I would prioritize these tasks:

1. Add authorization checks to user endpoints.
2. Add store/branch filtering to user listing and pagination.
3. Restrict branch information editing to explicit staff roles.
4. Protect `getCurrentUser()` against missing authentication.
5. Use update DTOs instead of saving client-provided User entities directly.

Next, the most useful file to inspect is your `JwtFilter.java` and current `SecurityConfig.java`. Those will let us verify whether the authentication flow actually connects correctly to `SecurityContextHolder`, and whether your endpoints are protected before they reach the controller.