# Bilix security hardening: simple before/after guide

This document records the minimum security fixes applied to the backend. The UI was not changed.

## 1. Never create a fake user when authentication is missing

### Before

```java
if (jwt == null || jwt.isEmpty()) {
    UserDto demoUser = new UserDto();
    demoUser.setId(1L);
    demoUser.setBranchId(1L);
    return ResponseEntity.ok(demoUser);
}
```

### After

```java
@GetMapping("/profile")
public ResponseEntity<UserDto> getUserProfile() {
    return ResponseEntity.ok(UserMapper.toDto(userService.getCurrentUser()));
}
```

Spring Security now rejects requests without a valid JWT. A missing token must never become a real branch identity.

## 2. Prevent user-IDOR (insecure direct object reference)

### Before

```java
public User getUserById(Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

### After

```java
public User getUserById(Long id) {
    User target = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    accessControlService.requireUserAccess(getCurrentUser(), target);
    return target;
}
```

The target user must be the current user, in the same branch/store, or the caller must be a super admin.

## 3. Protect global user listings

### Before

```java
return userRepository.findAll();
```

### After

```java
accessControlService.requireAdmin(getCurrentUser());
return userRepository.findAll();
```

The same check is applied to the paginated user listing. This avoids exposing every employee to ordinary branch users.

## 4. Validate null roles and relationships

### Before

```java
if (user.getRole() == UserRole.ROLE_ADMIN) { ... }
user.getBranch().getStore().getId();
```

### After

```java
if (user == null || user.getRole() == null) {
    throw new BadRequestException("Authenticated user is required");
}
if (user.getBranch() != null && user.getBranch().getStore() != null) {
    storeId = user.getBranch().getStore().getId();
}
```

This turns possible null-pointer failures into controlled authorization errors.

## 5. Customer branch isolation

Branch customer reads use branch-scoped repository queries. Customer update, delete, and detail access verify that the customer has an order in the current branch before continuing.

```java
if (!orderRepository.existsByCustomerIdAndBranchId(customerId, branchId)) {
    throw new AccessDeniedException("Customer is outside your branch");
}
```

This prevents a manager from changing another branch’s customer by guessing an ID.

## 6. Product store isolation

Product search now calls `requireStoreAccess(...)` before querying. The old fallback store ID (`1L`) and invalid-token testing behavior were removed.

## 7. Safe cache invalidation and transactions

User updates evict both user caches and are marked transactional:

```java
@Transactional
@CacheEvict(cacheNames = {"users", "usersAll"}, allEntries = true)
public User updateUser(User user) {
    return userRepository.save(user);
}
```

Cache invalidation prevents stale roles/branch assignments from remaining in memory. The transaction keeps the update atomic.

## What was intentionally not added

- No UI changes.
- No complex permission framework.
- No new external security library.
- No encryption changes, because passwords already use the configured password encoder.

## 8. Do not leak unexpected exception messages

### Before

```java
return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage());
```

### After

```java
log.error("Unexpected error occurred", ex);
return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
        "An unexpected error occurred");
```

The server log keeps the stack trace for debugging, but the client receives no SQL, database, provider, or implementation details. Payment controllers now follow the same rule.

## Verification

- Backend Maven compilation passed.
- Frontend production build passed.

The next sensible security step is automated authorization tests: branch A must receive `403` when requesting branch B users, customers, products, orders, refunds, or inventory.
