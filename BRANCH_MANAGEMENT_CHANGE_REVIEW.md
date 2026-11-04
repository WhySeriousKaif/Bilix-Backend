# Branch management changes

This is a short review of the branch changes made on 4 November 2026. The existing frontend screens and API paths remain the same. Branch update still accepts partial fields through `PUT`.

## 1. Branch reads always check access

**Before:** A cached response could skip the access check on later requests.

```java
@Cacheable(cacheNames = "branches", key = "#id")
public BranchDto getBranchById(Long id) {
    accessControlService.requireBranchAccess(userService.getCurrentUser(), id);
    // fetch branch
}
```

**After:** The service checks the user every time. The same fix applies to `getBranchesByStoreId`.

```java
public BranchDto getBranchById(Long id) {
    accessControlService.requireBranchAccess(userService.getCurrentUser(), id);
    Branch branch = branchRepository.findById(id).orElseThrow(...);
    return BranchMapper.toDto(branch);
}
```

## 2. Branch lists follow the user's assignment

**Before:** Every non-admin user was looked up as a store owner. Branch staff could get “Store not found.”

```java
Store store = storeRepository.findByStoreAdminId(user.getId());
```

**After:** Super Admin sees all branches; Store Admin and Store Manager see their store's branches; Branch Manager and Cashier see their assigned branch. Other roles are denied.

```java
if (role == UserRole.ROLE_BRANCH_MANAGER || role == UserRole.ROLE_BRANCH_CASHIER
        || role == UserRole.ROLE_CASHIER) {
    return List.of(getBranchById(user.getBranch().getId()));
}
```

## 3. Creating a branch uses server-owned identity and dates

**Before:** The mapper accepted an ID and timestamps from the request.

```java
.id(branchDto.getId())
.createdAt(branchDto.getCreatedAt())
.updatedAt(branchDto.getUpdatedAt())
```

**After:** The mapper leaves those fields unset. JPA generates the ID, and `@PrePersist` sets both dates. Creation also explicitly requires `ROLE_STORE_ADMIN`, validates a nonblank name, and returns HTTP 201 with the same response body.

```java
if (currentUser.getRole() != UserRole.ROLE_STORE_ADMIN) {
    throw new BadRequestException("Only a Store Admin can create a branch for their store");
}
return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(branchDto));
```

## 4. Deleting a branch preserves employee accounts

**Before:** Deleting a branch could cascade deletion to its manager user.

```java
@OneToOne(cascade = CascadeType.REMOVE)
private User manager;
```

**After:** The cascade is removed. A branch with assigned employees must have them reassigned before deletion.

```java
@OneToOne
private User manager;

if (!userRepository.findByBranchId(id).isEmpty()) {
    throw new BadRequestException("Reassign branch employees before deleting this branch");
}
```

## Kept as-is

- The frontend UI, API URLs, response fields, and partial update behavior are unchanged.
- The existing pessimistic lock method remains available for operations that need it; ordinary branch reads do not acquire a write lock.
- Branch manager assignment still uses the existing employee service. This change does not introduce another assignment path.

## Checks

- `mvn -q -Dtest=BranchServiceImpTest test` passed.
- `mvn -q test` passed.
- `npm run build` passed (Vite reported only bundle size and import warnings).
