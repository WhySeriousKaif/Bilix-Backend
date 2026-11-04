# Employee changes: three safeguards

The employee screens and normal create, edit, and delete flow stay the same. These checks only reject unsafe requests.

## 1. Do not create an admin as an employee

Before, the store employee endpoint accepted nearly any role. Now it accepts only Store Manager, Store Employee, Branch Manager, or Branch Cashier. An edit also cannot promote someone to Super Admin or Store Admin.

```java
// Before: other roles fell through as store employees.
boolean branchRole = role == ROLE_BRANCH_MANAGER || role == ROLE_BRANCH_CASHIER;

// After: check the allowed employee roles first.
if (!STORE_CREATION_ROLES.contains(employee.getRole())) {
    throw new BadRequestException("Invalid role for store employee");
}
```

## 2. Keep employees in the correct store

Before, an employee could have Store 10 but a branch belonging to Store 20. Now the two must match.

```java
// Before: the branch was loaded without checking its store.
branch = branchRepository.findByIdForUpdate(branchId).orElseThrow(...);

// After: verify the parent store before saving the employee.
if (branch.getStore() == null || !storeId.equals(branch.getStore().getId())) {
    throw new BadRequestException("Selected branch does not belong to this store");
}
```

## 3. Keep one manager per branch

Before, promoting an existing branch employee without sending `branchId` could replace the current manager. Now the service checks the employee's current branch too.

```java
// Before: availability was checked only when branchId was sent.
if (employeeDetails.getBranchId() != null) {
    ensureBranchManagerAvailable(role, targetBranch, existingUser);
}

// After: check whenever the resulting role is Branch Manager.
if (nextRole == UserRole.ROLE_BRANCH_MANAGER) {
    ensureBranchManagerAvailable(nextRole, targetBranch, existingUser);
}
```

## Verification

The targeted employee tests, full backend tests, and frontend build passed after the changes above.
