# Authorization tests: simple before/after guide

## What was missing before

Authorization existed in `AccessControlService`, but there was no focused test proving the important boundaries.

```java
// Code could be changed later without a test catching a cross-branch regression.
accessControlService.requireBranchAccess(user, branchId);
```

## What was added

```java
@Test
void branchManagerCannotAccessAnotherBranch() {
    User manager = user(100L, ROLE_BRANCH_MANAGER, branchA, branchA.getStore());

    assertThrows(BadRequestException.class,
            () -> accessControlService.requireBranchAccess(manager, 2L));
}
```

The test creates a manager assigned to branch A and asks for branch B. The authorization check must reject the request.

## Covered cases

- Branch manager can access the assigned branch.
- Branch manager cannot access another branch.
- Store manager can access a branch in the same store.
- Store manager cannot access another store.
- Cashier cannot manage employees.
- A user can access their own user record.
- A user cannot access a user from another branch.

## Why this is useful

The test is small because it tests one class directly. It does not need a browser, database, or frontend. If somebody accidentally removes an authorization call later, these tests fail immediately.

## Interview explanation

> “I test authorization boundaries, not only successful business logic. The important rule is that the caller’s role and tenant relationship are checked before returning data.”

## 400 versus 403

These service tests assert rejection with `BadRequestException`, which is the project’s current access-control exception. At the HTTP/security layer, invalid credentials should be `401`, and authenticated users without permission should be `403`. A later controller integration-test layer can verify those exact HTTP status codes.
