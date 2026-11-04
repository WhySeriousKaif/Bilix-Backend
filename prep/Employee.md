# Employee Management module

This module handles the employee lifecycle in your POS application: creating employees, assigning them to a store or branch, updating their details and roles, deleting them, and fetching employee lists.

The architecture follows the same pattern as your Store and Branch modules:

```
Client / Postman
      ↓
EmployeeController
      ↓
EmployeeService (interface)
      ↓
EmployeeServiceImpl (business logic)
      ↓
UserRepository / StoreRepository / BranchRepository
      ↓
Database
```

One important thing to understand: in your project, an employee is stored in the `User` table, not in a separate `Employee` table. The employee's role and store/branch relationships determine what kind of employee they are.

Let's go through the code in order.

## 1. EmployeeController.java — API layer

The controller receives HTTP requests and forwards them to the service. It does not contain the actual employee creation or authorization logic.

### Class annotations

```
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
}
```

- `@RestController`: tells Spring that this class handles REST API requests and returns response data as JSON.
- `@RequestMapping("/api/employees")`: defines the common URL prefix for all endpoints.
- `@RequiredArgsConstructor`: Lombok generates a constructor for the `final` fields.
- `EmployeeService`: injected through constructor injection.

### A. Create a store employee

```
@PostMapping("/store/{storeId}")
public ResponseEntity<UserDto> createStoreEmployee(
        @PathVariable("storeId") Long storeId,
        @RequestBody UserDto employee) throws UserException {

    UserDto createdEmployee =
            employeeService.createStoreEmployee(employee, storeId);

    return ResponseEntity.ok(createdEmployee);
}
```

Example request:

```
POST /api/employees/store/10
```

Example JSON body:

```
{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "password": "password123",
  "phone": "9876543210",
  "role": "ROLE_STORE_MANAGER"
}
```

What happens:

1. `{storeId}` is extracted from the URL using `@PathVariable`.
2. The JSON request body is converted into a `UserDto` using `@RequestBody`.
3. The controller calls `createStoreEmployee(employee, storeId)`.
4. The service performs authorization, validation, entity creation and database saving.
5. The returned `UserDto` is sent to the client.

Why use `ResponseEntity`?

It gives you control over the HTTP status code and response body.

Your code currently returns `200 OK`. Since this endpoint creates a new employee, `201 Created` is more appropriate:

```
return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
```

### B. Create a branch employee

```
@PostMapping("/branch/{branchId}")
public ResponseEntity<UserDto> createBranchEmployee(
        @PathVariable("branchId") Long branchId,
        @RequestBody UserDto employeeDetails) throws UserException {

    UserDto createdEmployee =
            employeeService.createBranchEmployee(employeeDetails, branchId);

    return ResponseEntity.ok(createdEmployee);
}
```

Example:

```
POST /api/employees/branch/25
```

This endpoint creates an employee specifically for branch `25`.

The main difference:

| Store employee endpoint                               | Branch employee endpoint                                           |
| ----------------------------------------------------- | ------------------------------------------------------------------ |
| Uses `storeId`                                        | Uses `branchId`                                                    |
| Assigns employee to a store                           | Assigns employee to a branch and its store                         |
| Can create certain store-level or branch-scoped roles | Your implementation allows branch manager and branch cashier roles |

We'll see exactly how the service enforces these differences.

### C. Update employee

```
@PutMapping("/{employeeId}")
public ResponseEntity<UserDto> updateEmployee(
        @PathVariable("employeeId") Long employeeId,
        @RequestBody UserDto employeeDetails) throws UserException {

    UserDto updatedEmployee =
            employeeService.updateEmployee(employeeDetails, employeeId);

    return ResponseEntity.ok(updatedEmployee);
}
```

Example:

```
PUT /api/employees/15
```

The URL identifies the employee being updated. The body contains the new details.

Notice that the employee ID comes from the URL, not from the JSON body. That's a good API design choice.

### D. Delete employee

```
@DeleteMapping("/{employeeId}")
public ResponseEntity<ApiResponse> deleteEmployee(
        @PathVariable("employeeId") Long employeeId) throws UserException {

    employeeService.deleteEmployee(employeeId);

    ApiResponse apiResponse =
            new ApiResponse("Employee deleted successfully");

    return ResponseEntity.ok(apiResponse);
}
```

Example:

```
DELETE /api/employees/15
```

The service deletes the employee, and the controller returns a success message.

You could alternatively return `204 No Content` without a response body. Your current `200 OK` with an `ApiResponse` is also a valid API design.

### E. Get all employees of a store

```
@GetMapping("/store/{storeId}")
public ResponseEntity<List<UserDto>> findStoreEmployees(
        @PathVariable("storeId") Long storeId,
        @RequestParam(required = false) UserRole role)
        throws UserException {

    List<UserDto> employees =
            employeeService.findStoreEmployees(storeId, role);

    return ResponseEntity.ok(employees);
}
```

Example without role filtering:

```
GET /api/employees/store/10
```

Example with role filtering:

```
GET /api/employees/store/10?role=ROLE_STORE_MANAGER
```

Here is an important distinction:

- `@PathVariable` extracts data from the URL path.
- `@RequestParam` extracts a query parameter.

Because `role` is optional, `required = false` means the request can omit it.

Spring automatically converts the query parameter into your `UserRole` enum, provided that its value matches an enum constant.

### F. Get employees of a branch

```
@GetMapping("/branch/{branchId}")
public ResponseEntity<List<UserDto>> findBranchEmployees(
        @PathVariable("branchId") Long branchId,
        @RequestParam(required = false) UserRole role)
        throws UserException {
```

Example:

```
GET /api/employees/branch/25
```

Or:

```
GET /api/employees/branch/25?role=ROLE_BRANCH_CASHIER
```

This endpoint gets employees assigned to a particular branch, optionally filtering by role.

## 2. EmployeeService.java — Interface

Your interface defines the operations that an employee service must provide:

```
public interface EmployeeService {
    UserDto createStoreEmployee(UserDto employee, Long storeId)
            throws UserException;

    UserDto createBranchEmployee(UserDto employeeDetails, Long branchId)
            throws UserException;

    UserDto updateEmployee(UserDto employeeDetails, Long employeeId)
            throws UserException;

    void deleteEmployee(Long employeeId)
            throws UserException;

    List<UserDto> findStoreEmployees(Long storeId, UserRole role)
            throws UserException;

    List<UserDto> findBranchEmployees(Long branchId, UserRole role)
            throws UserException;
}
```

Why do we need an interface?

The controller depends on the `EmployeeService` abstraction rather than directly depending on `EmployeeServiceImpl`.

This gives you a separation between the API layer and the business logic implementation.

For example:

```
private final EmployeeService employeeService;
```

Spring injects the implementation:

```
@Service
public class EmployeeServiceImpl implements EmployeeService {
    // Actual business logic
}
```

The controller doesn't need to know how the employee is saved or how permissions are checked.

## 3. EmployeeServiceImpl.java — Actual business logic

This is the most important class. It decides:

- Who is allowed to create or modify an employee.
- Which store or branch the employee belongs to.
- Which employee roles are allowed.
- What happens when a branch manager is replaced or deleted.

### A. Dependencies and annotations

Your implementation starts with:

```
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AccessControlService accessControlService;
}
```

Let's understand why every dependency is required.

| Dependency             | Purpose                                                         |
| ---------------------- | --------------------------------------------------------------- |
| `UserRepository`       | Save, update, delete and fetch employees.                       |
| `StoreRepository`      | Find the store to which an employee belongs.                    |
| `BranchRepository`     | Find branches and assign branch managers.                       |
| `PasswordEncoder`      | Hash employee passwords before saving.                          |
| `UserService`          | Get the currently authenticated user.                           |
| `AccessControlService` | Check whether that user has permission to perform an operation. |

Notice that you're reusing the `User` entity and `UserRepository`. That's why there's no separate `EmployeeRepository`.

Behind the scenes: When Spring starts, it discovers `@Service`, creates an instance of `EmployeeServiceImpl`, injects its dependencies and registers it as the implementation of `EmployeeService`.

### B. Creating a store employee

Let's break down your first service method.

```
@Override
@CacheEvict(
    cacheNames = {
        "users", "usersAll", "branches", "branchesByStore"
    },
    allEntries = true
)
@Transactional
public UserDto createStoreEmployee(UserDto employee, Long storeId)
        throws UserException {
```

Three annotations are important here.

`@Override`

This method implements the `createStoreEmployee()` method declared in the interface.

`@Transactional`

The method runs inside a database transaction.

Imagine creating an employee and assigning that employee as a branch manager. You are potentially performing two database writes:

1. Save the employee.
2. Update the branch's manager reference.

A transaction ensures that these operations are treated as one unit. If an operation fails and the transaction rolls back, you don't end up with only half of the intended changes.

`@CacheEvict`

After a successful operation, Spring clears the specified caches. This prevents old user and branch data from continuing to appear after a new employee has been created.

`allEntries = true` means the entire cache is cleared, rather than just one entry.

#### Step 1: Check who is creating the employee

```
accessControlService.requireManagerAccessToStore(
    userService.getCurrentUser(), storeId
);
```

This is an important security check.

First:

```
userService.getCurrentUser()
```

gets the currently authenticated user, typically using Spring Security's `SecurityContextHolder`.

Then:

```
requireManagerAccessToStore(currentUser, storeId)
```

checks whether that user has manager-level permission over the requested store.

For example:

Imagine this setup:

```
Store 10
   ├── Branch 101
   └── Branch 102

Store 20
   └── Branch 201
```

A Store Manager of Store 10 should not be able to create an employee under Store 20.

This check is intended to prevent that kind of unauthorized access.

#### Step 2: Find the store

```
Store store = storeRepository.findById(storeId)
    .orElseThrow(() -> new RuntimeException("Store not found"));
```

`findById()` returns an `Optional<Store>`.

The store may exist, or it may not.

- If found, you get the `Store` object.
- If not found, `orElseThrow()` throws an exception.

For example, if `storeId = 10`, this fetches the Store entity with ID 10.

A small improvement: use your `NotFoundException` instead of `RuntimeException`, so your existing global exception handler can return the correct not-found response.

#### Step 3: Decide whether this employee needs a branch

```
Branch branch = null;

boolean isBranchScopedRole =
        employee.getRole() == UserRole.ROLE_BRANCH_MANAGER
        || employee.getRole() == UserRole.ROLE_BRANCH_CASHIER;
```

This is a role-based decision.

Your code considers these two roles branch-scoped:

- `ROLE_BRANCH_MANAGER`
- `ROLE_BRANCH_CASHIER`

Other roles are treated as store-level roles in this method.

For example:

```
Store Manager
    store = Store 10
    branch = null

Branch Manager
    store = Store 10
    branch = Branch 101

Branch Cashier
    store = Store 10
    branch = Branch 101
```

The `branch` variable initially remains `null` because a store-level employee doesn't necessarily belong to one particular branch.

#### Step 4: Validate the branch assignment

```
if (isBranchScopedRole) {
    if (employee.getBranchId() == null) {
        throw new UserException("Branch ID is required for this role");
    }

    branch = branchRepository.findByIdForUpdate(employee.getBranchId())
        .orElseThrow(() -> new RuntimeException("Branch not found"));

    ensureBranchManagerAvailable(employee.getRole(), branch, null);
}
```

There are three things happening here.

First, branch-scoped roles must have a `branchId`.

Second, the branch is fetched using:

```
findByIdForUpdate()
```

Remember the pessimistic locking method in your `BranchRepository`?

It uses a database write lock, conceptually similar to:

```
SELECT *
FROM branch
WHERE id = ?
FOR UPDATE;
```

This is useful because two managers might try to assign themselves as the manager of the same branch at almost the same time.

The lock helps serialize those competing operations.

Third:

```
ensureBranchManagerAvailable(employee.getRole(), branch, null);
```

checks whether the branch already has a manager.

We'll examine this helper method shortly.

#### Step 5: Convert DTO into an entity

```
User user = UserMapper.toEntity(employee, passwordEncoder);

user.setStore(store);
user.setBranch(branch);
```

This is the DTO-to-entity conversion.

The incoming object is a `UserDto`, but your database stores a `User` entity.

The mapper creates the entity, and the service sets its relationships.

For example, the resulting object might look conceptually like this:

```
User
 ├── fullName: Rahul Sharma
 ├── email: rahul@example.com
 ├── role: ROLE_BRANCH_MANAGER
 ├── store: Store 10
 └── branch: Branch 101
```

The `PasswordEncoder` is passed to the mapper, presumably so that the password can be encoded during conversion. Check `UserMapper.toEntity()` to confirm exactly how it handles passwords.

#### Step 6: Save the employee

```
User savedUser = userRepository.save(user);
```

Spring Data JPA saves the entity to the database.

Conceptually, the resulting SQL is similar to:

```
INSERT INTO users (
    full_name,
    email,
    password,
    role,
    store_id,
    branch_id
)
VALUES (?, ?, ?, ?, ?, ?);
```

The actual SQL depends on your table and column names.

#### Step 7: Assign the employee as branch manager

```
if (savedUser.getRole() == UserRole.ROLE_BRANCH_MANAGER
        && branch != null) {

    branch.setManager(savedUser);
    branchRepository.save(branch);
}
```

This part is particularly important.

You have two relationships to maintain:

- `User.branch` tells us which branch the employee belongs to.
- `Branch.manager` identifies the manager assigned to that branch.

So when you create a branch manager, you update both sides.

Conceptually:

```
Branch 101
   └── manager → Rahul (User ID 50)

Rahul (User ID 50)
   ├── store → Store 10
   └── branch → Branch 101
```

Finally:

```
return UserMapper.toDto(savedUser);
```

You convert the saved entity back into a DTO before returning it to the controller.

Complete flow:

```
POST /api/employees/store/10
             ↓
Get authenticated user
             ↓
Check access to Store 10
             ↓
Fetch Store 10
             ↓
Check employee's role
             ↓
Fetch and validate branch, if required
             ↓
Convert UserDto → User
             ↓
Save employee
             ↓
Assign branch manager, if applicable
             ↓
Convert User → UserDto
             ↓
Return response
```

Important design observation

There are two improvements I would make here:

1. Explicitly allow only the intended store-level roles and branch-level roles. Currently, anything other than the two branch-scoped roles bypasses the branch requirement. That means an unintended role could potentially be created through this method.
2. When a branch ID is supplied, verify that the branch actually belongs to the `storeId` in the URL. Currently, the code fetches the branch but doesn't visibly compare its store with the requested store. Without that check, a user with access to one store could potentially create an employee whose `store` and `branch` point to different stores.

Both are important tenant-isolation checks.&#x20;

Pasted text(20261104-111937).txt



### C. Creating a branch employee

```
@Override
@CacheEvict(
    cacheNames = {
        "users", "usersAll", "branches", "branchesByStore"
    },
    allEntries = true
)
@Transactional
public UserDto createBranchEmployee(UserDto employeeDetails, Long branchId)
        throws UserException {
```

This method is similar to the previous one, but the URL directly identifies the branch.

#### Step 1: Fetch the branch with a lock

```
Branch branch = branchRepository.findByIdForUpdate(branchId)
    .orElseThrow(() -> new RuntimeException("Branch not found"));
```

Why use the lock?

Because you want to avoid two simultaneous requests successfully assigning different managers to the same branch.

#### Step 2: Get the current user and check permissions

```
User currentUser = userService.getCurrentUser();

accessControlService.requireManagerAccessToBranch(
    currentUser, branch
);
```

This checks whether the authenticated user can manage the requested branch.

For example:

- Super Admin: can manage it.
- Store Admin or Store Manager: can manage branches in their store, according to your access-control rules.
- Branch Manager: can manage their own branch, according to your rules.
- Ordinary cashier: should be denied.

#### Step 3: Check the requested role

```
if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_CASHIER
        || employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
```

This is an explicit role allowlist.

Unlike `createStoreEmployee()`, this method allows only two specific roles.

That's good because not every role should be assignable through a branch-employee endpoint.

#### Step 4: Check whether the branch already has a manager

```
ensureBranchManagerAvailable(
    employeeDetails.getRole(), branch, null
);
```

If the role is `ROLE_BRANCH_MANAGER`, the helper checks whether that branch already has a manager.

A branch should not accidentally receive two managers through concurrent or repeated requests.

#### Step 5: Create and assign the employee

```
User user = UserMapper.toEntity(employeeDetails, passwordEncoder);

user.setBranch(branch);

// Set store from branch
user.setStore(branch.getStore());

User savedUser = userRepository.save(user);
```

Notice something important:

```
user.setStore(branch.getStore());
```

You don't trust the client to tell you which store the branch belongs to.

You fetch the branch and get its associated store from the database.

This is a good design decision.

For example:

```
Branch 101 belongs to Store 10

Employee created for Branch 101:

User.branch = Branch 101
User.store  = Store 10
```

Then:

```
if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
    branch.setManager(savedUser);
    branchRepository.save(branch);
}
```

If this employee is a branch manager, you also update the branch's manager relationship.

Finally, if the role isn't allowed:

```
throw new UserException("Invalid role for branch employee");
```

The method rejects the request.

### D. The `ensureBranchManagerAvailable()` helper

```
private void ensureBranchManagerAvailable(
        UserRole role,
        Branch branch,
        User currentEmployee) throws UserException {

    if (role != UserRole.ROLE_BRANCH_MANAGER
            || branch.getManager() == null) {
        return;
    }

    if (currentEmployee != null
            && branch.getManager().getId().equals(currentEmployee.getId())) {
        return;
    }

    throw new UserException(
        "This branch already has a manager. Edit or remove the current manager before assigning another."
    );
}
```

This is a reusable helper method.

Let's understand its logic.

Condition 1:

```
if (role != UserRole.ROLE_BRANCH_MANAGER
        || branch.getManager() == null) {
    return;
}
```

The method exits immediately in either situation:

- The employee isn't a branch manager.
- The branch doesn't currently have a manager.

So the remaining code executes only when someone is trying to assign a branch manager and the branch already has one.

Condition 2:

```
if (currentEmployee != null
        && branch.getManager().getId().equals(currentEmployee.getId())) {
    return;
}
```

This supports updating the existing manager.

For example:

```
Branch 101
    Current manager: Rahul (ID 50)
```

Rahul is being edited without replacing him with another manager.

The method allows this because the existing manager and the employee being updated have the same ID.

Otherwise:

```
throw new UserException(...);
```

The operation is rejected.

The `currentEmployee` parameter is useful because:

- During creation, it is `null`.
- During an update, it can contain the existing employee.

This prevents the helper from rejecting an existing manager just because the branch already has a manager.

### E. Updating an employee

This is one of the more interesting methods because it handles both ordinary employee updates and employee transfers between branches.

Your method:

```
@Override
@CacheEvict(
    cacheNames = {
        "users", "usersAll", "branches", "branchesByStore"
    },
    allEntries = true
)
@Transactional
public UserDto updateEmployee(UserDto employeeDetails, Long employeeId)
        throws UserException {
```

#### Step 1: Find the existing employee

```
User existingUser = userRepository.findById(employeeId)
    .orElseThrow(() ->
        new RuntimeException("Employee not found with this id"));
```

You first fetch the existing employee from the database.

Why?

Because you should update the existing database entity rather than create a completely new `User`.

For example, suppose employee ID 50 currently has:

```
Name: Rahul
Email: rahul@example.com
Role: ROLE_BRANCH_CASHIER
Store: 10
Branch: 101
```

The request might only change the name and phone number.

You fetch the existing record and change the required fields.

#### Step 2: Check authorization for the employee's current location

```
User currentUser = userService.getCurrentUser();

Branch previousBranch = existingUser.getBranch();

boolean wasBranchManager =
        existingUser.getRole() == UserRole.ROLE_BRANCH_MANAGER;
```

You get the authenticated user and remember two things:

- The employee's current branch.
- Whether this employee is currently a branch manager.

These values are needed later when you handle transfers and manager reassignment.

Now the permission checks:

```
if (existingUser.getBranch() != null) {
    accessControlService.requireManagerAccessToBranch(
        currentUser, existingUser.getBranch()
    );

} else if (existingUser.getStore() != null) {
    accessControlService.requireManagerAccessToStore(
        currentUser, existingUser.getStore().getId()
    );

} else {
    accessControlService.requireAdmin(currentUser);
}
```

This is an important security pattern: check permission against the employee's existing resource before modifying that resource.

For example:

```
Employee 50
    Store: 10
    Branch: 101
```

A Store Manager of Store 20 cannot update this employee just by passing employee ID 50.

The service checks access to Branch 101 first.

If the employee has no branch but belongs to a store, it checks access to that store.

If neither relationship exists, only an admin is allowed.

#### Step 3: Check permission for the destination branch

```
if (employeeDetails.getBranchId() != null) {

    Branch targetBranch = branchRepository.findByIdForUpdate(
        employeeDetails.getBranchId()
    ).orElseThrow(() -> new RuntimeException("Branch not found"));

    accessControlService.requireManagerAccessToBranch(
        currentUser, targetBranch
    );

    ensureBranchManagerAvailable(
        employeeDetails.getRole(), targetBranch, existingUser
    );
}
```

This is another very important security check.

Why are there two authorization checks?

Because an employee might move from one branch to another.

Imagine:

```
Current employee:
    Branch 101 (Store 10)

Requested change:
    Branch 201 (Store 20)
```

The service checks access to:

1. The employee's current branch.
2. The destination branch.

Without checking the destination, a Store Manager could potentially take an employee from another store and assign them to a location they don't control.

Your code also locks the destination branch and checks whether it already has a manager if the employee's requested role is `ROLE_BRANCH_MANAGER`.

#### Step 4: Update employee details

```
existingUser.setFullName(employeeDetails.getFullName());
existingUser.setEmail(employeeDetails.getEmail());
existingUser.setPhone(employeeDetails.getPhone());
existingUser.setRole(employeeDetails.getRole());
```

These statements update the existing entity in memory.

At this point, the changes haven't necessarily been written to the database.

JPA tracks changes to managed entities, and the transaction will persist them when it flushes.

#### Step 5: Update password only when provided

```
if (employeeDetails.getPassword() != null
        && !employeeDetails.getPassword().isEmpty()) {

    existingUser.setPassword(
        passwordEncoder.encode(employeeDetails.getPassword())
    );
}
```

This is a good approach.

Suppose the client wants to change only the employee's name.

It sends:

```
{
  "fullName": "Rahul Kumar",
  "email": "rahul@example.com",
  "phone": "9999999999",
  "role": "ROLE_BRANCH_CASHIER"
}
```

There is no password.

Your service leaves the existing password unchanged.

But when a new password is provided, you encode it before saving.

Never store the raw password directly.

#### Step 6: Update branch and store assignment

```
if (employeeDetails.getBranchId() != null) {

    Branch branch = branchRepository.findById(
        employeeDetails.getBranchId()
    ).orElseThrow(() -> new RuntimeException("Branch not found"));

    existingUser.setBranch(branch);

    // Update store from branch
    existingUser.setStore(branch.getStore());
}
```

This is the actual transfer operation.

For example:

Before:

```
Rahul
    Store: 10
    Branch: 101
```

After moving him:

```
Rahul
    Store: 20
    Branch: 201
```

The store is updated from the selected branch.

That's good because the branch determines its parent store.

One small inefficiency: you already fetched `targetBranch` using `findByIdForUpdate()` in the previous step. You could reuse that object rather than fetching the same branch again.

#### Step 7: Remove the old manager reference when necessary

```
Branch newBranch = existingUser.getBranch();

if (wasBranchManager && previousBranch != null
        && (employeeDetails.getRole() != UserRole.ROLE_BRANCH_MANAGER
            || previousBranch != newBranch)) {

    if (previousBranch.getManager() != null
            && previousBranch.getManager().getId()
                    .equals(existingUser.getId())) {

        previousBranch.setManager(null);
        branchRepository.save(previousBranch);
    }
}
```

This is important for maintaining consistency.

Imagine Rahul is the manager of Branch 101.

Now you update Rahul to become a cashier.

If you only update Rahul's role, the branch might still point to him as its manager:

```
Branch 101
    manager → Rahul

Rahul
    role → ROLE_BRANCH_CASHIER
```

That would be inconsistent.

Your code clears the old manager reference when:

- The employee is no longer a branch manager, or
- The employee has moved to another branch.

It also checks whether the branch's current manager is actually this employee before clearing it. That's a sensible safeguard.

#### Step 8: Save the employee and assign the new manager

```
User savedUser = userRepository.save(existingUser);

if (savedUser.getRole() == UserRole.ROLE_BRANCH_MANAGER
        && savedUser.getBranch() != null) {

    savedUser.getBranch().setManager(savedUser);

    branchRepository.save(savedUser.getBranch());
}
```

Now the updated employee is saved.

If the employee is still a branch manager and has a branch, you update that branch's manager reference.

Finally:

```
return UserMapper.toDto(savedUser);
```

The saved entity is converted back into a DTO.

Let's walk through a complete manager-transfer example:

Initially:

```
Branch 101
    Manager: Rahul (ID 50)

Branch 201
    Manager: None
```

You update Rahul and assign him to Branch 201.

The service:

1. Fetches Rahul.
2. Checks permission over Branch 101.
3. Fetches Branch 201 with a lock.
4. Checks permission over Branch 201.
5. Checks that Branch 201 has no other manager.
6. Changes Rahul's branch to Branch 201.
7. Changes Rahul's store to the store associated with Branch 201.
8. Clears Rahul from Branch 101's manager reference.
9. Saves Rahul.
10. Sets Rahul as the manager of Branch 201.

Final result:

```
Branch 101
    Manager: None

Branch 201
    Manager: Rahul (ID 50)
```

This is a good example of why the `@Transactional` annotation is useful: the employee and branch relationships are changed as one logical operation.

Pasted text(20261104-111937).txt

### F. Deleting an employee

```
@Override
@CacheEvict(
    cacheNames = {
        "users", "usersAll", "branches", "branchesByStore"
    },
    allEntries = true
)
@Transactional
public void deleteEmployee(Long employeeId) throws UserException {
```

#### Step 1: Fetch the employee

```
User existingUser = userRepository.findById(employeeId)
    .orElseThrow(() -> new RuntimeException("Employee not found"));
```

You cannot delete an employee who doesn't exist.

#### Step 2: Prevent users from deleting themselves

```
User currentUser = userService.getCurrentUser();

if (currentUser.getId().equals(existingUser.getId())) {
    throw new UserException("You cannot delete your own account");
}
```

For example, imagine a Store Admin is logged in and attempts to delete their own account.

Your code prevents this.

This is useful because deleting the current account might leave a store without its administrator.

#### Step 3: Check authorization

```
if (existingUser.getBranch() != null) {
    accessControlService.requireManagerAccessToBranch(
        currentUser, existingUser.getBranch()
    );

} else if (existingUser.getStore() != null) {
    accessControlService.requireManagerAccessToStore(
        currentUser, existingUser.getStore().getId()
    );

} else {
    accessControlService.requireAdmin(currentUser);
}
```

Again, you check permissions based on the employee's current branch or store.

An employee assigned to Store 10 should not be deletable by an ordinary manager of Store 20.

#### Step 4: Clear the branch manager reference

```
if (existingUser.getBranch() != null) {

    Branch branch = branchRepository.findByIdForUpdate(
        existingUser.getBranch().getId()
    ).orElse(null);

    if (branch != null && branch.getManager() != null
            && branch.getManager().getId().equals(existingUser.getId())) {

        branch.setManager(null);
        branchRepository.save(branch);
    }
}
```

Why do this?

Suppose Rahul is the manager of Branch 101.

Before deleting Rahul:

```
Branch 101
    manager → Rahul
```

You first remove that reference:

```
Branch 101
    manager → null
```

This avoids leaving a branch that points to an employee you are about to delete.

It is particularly important because your `Branch` entity has a relationship to `User` through `Branch.manager`.

#### Step 5: Delete the user

```
userRepository.delete(existingUser);
```

Now the employee is deleted from the database.

One practical consideration for a POS system: employees may appear in historical orders, shifts, refunds or audit records. Physical deletion could cause foreign-key problems or remove important business history. In a production POS, consider disabling an employee account with an `active` or `employmentStatus` field instead of physically deleting it.

Pasted text(20261104-111937).txt

### G. Find employees belonging to a store

```
@Override
public List<UserDto> findStoreEmployees(Long storeId, UserRole role)
        throws UserException {
```

#### Step 1: Check access

```
accessControlService.requireStoreAccess(
    userService.getCurrentUser(), storeId
);
```

Only users authorized to access that store should be able to list its employees.

#### Step 2: Fetch the store

```
Store store = storeRepository.findById(storeId)
    .orElseThrow(() -> new RuntimeException("Store not found"));
```

#### Step 3: Fetch employees

```
return userRepository.findByStore(store)
    .stream()
    .filter(user -> role == null || user.getRole().equals(role))
    .map(UserMapper::toDto)
    .collect(Collectors.toList());
```

Let's understand this stream.

Imagine the database returns:

```
Rahul → ROLE_STORE_MANAGER
Amit  → ROLE_BRANCH_MANAGER
Sara  → ROLE_BRANCH_CASHIER
John  → ROLE_STORE_EMPLOYEE
```

When `role == null`, this condition:

```
role == null || user.getRole().equals(role)
```

is always true, so all employees are included.

When the requested role is `ROLE_STORE_MANAGER`, only Rahul is included.

Then:

```
.map(UserMapper::toDto)
```

converts every `User` entity into a `UserDto`.

Finally:

```
.collect(Collectors.toList())
```

collects the results into a list.

A performance improvement: you're fetching all employees for that store and filtering them in Java. For a large store, it is better to filter in the database using a repository method such as:

```
List<User> findByStoreAndRole(Store store, UserRole role);
```

You can retain `findByStore(store)` when no role filter is provided.

Pasted text(20261104-111937).txt

### H. Find employees belonging to a branch

```
@Override
public List<UserDto> findBranchEmployees(Long branchId, UserRole role)
        throws UserException {
```

First:

```
accessControlService.requireBranchAccess(
    userService.getCurrentUser(), branchId
);
```

This checks whether the current user can access the requested branch.

Then:

```
branchRepository.findById(branchId)
    .orElseThrow(() -> new RuntimeException("Branch not found"));
```

This verifies that the branch exists.

Next:

```
List<User> employees = userRepository.findByBranchId(branchId);
```

This fetches users assigned to that branch.

Now comes an interesting part: the role filter.

```
return employees.stream()
    .filter(user -> {
        if (role == null)
            return true;

        if (user.getRole().equals(role))
            return true;

        if (role == UserRole.ROLE_CASHIER
                && user.getRole() == UserRole.ROLE_BRANCH_CASHIER) {
            return true;
        }

        return false;
    })
    .map(UserMapper::toDto)
    .collect(Collectors.toList());
```

You're implementing an alias.

For example, when the client requests:

```
GET /api/employees/branch/101?role=ROLE_CASHIER
```

Your code includes both:

```
ROLE_CASHIER
ROLE_BRANCH_CASHIER
```

But when the client requests:

```
GET /api/employees/branch/101?role=ROLE_BRANCH_MANAGER
```

It includes only branch managers.

This is a useful feature when you want the API to treat general cashiers and branch cashiers as part of one cashier category in a particular search.

However, make sure that this is the behavior your business rules actually intend. These are still two distinct enum roles.

Pasted text(20261104-111937).txt

## 4. Important improvements I'd make before considering this production-ready

The overall structure is good. You're checking authorization in the service layer, using transactions for multi-step operations, encoding passwords and maintaining the branch-manager relationship.

However, there are some important edge cases worth fixing.

### 1. Explicitly validate employee roles

High priority

In `createStoreEmployee()`, you only identify two branch-scoped roles:

```
boolean isBranchScopedRole =
        employee.getRole() == UserRole.ROLE_BRANCH_MANAGER
        || employee.getRole() == UserRole.ROLE_BRANCH_CASHIER;
```

Everything else bypasses the branch requirement.

That means a request could potentially create an employee with an unintended role, depending on what the access-control check allows.

Create explicit allowlists:

```
private static final Set<UserRole> STORE_ROLES = Set.of(
    UserRole.ROLE_STORE_MANAGER,
    UserRole.ROLE_STORE_EMPLOYEE
);

private static final Set<UserRole> BRANCH_ROLES = Set.of(
    UserRole.ROLE_BRANCH_MANAGER,
    UserRole.ROLE_BRANCH_CASHIER
);
```

Then validate the requested role against the appropriate set.

### 2. Verify that a branch belongs to the requested store

High priority

In `createStoreEmployee()`, you fetch a branch using:

```
branchRepository.findByIdForUpdate(employee.getBranchId())
```

But you don't verify:

```
branch.getStore().getId().equals(storeId)
```

Without this check, a request could potentially create this inconsistent relationship:

```
User
 ├── store: Store 10
 └── branch: Branch 201

Branch 201
 └── store: Store 20
```

The employee would belong to Store 10 but be assigned to a branch belonging to Store 20.

Always validate this relationship.

### 3. Fix manager assignment during employee updates

High priority

This is probably the most important edge case in your update method.

You call:

```
ensureBranchManagerAvailable(
    employeeDetails.getRole(),
    targetBranch,
    existingUser
);
```

But this happens only when:

```
employeeDetails.getBranchId() != null
```

Consider this scenario:

```
Branch 101
    Manager: Amit

Rahul
    Role: ROLE_BRANCH_CASHIER
    Branch: 101
```

Now Rahul is updated to:

```
Role: ROLE_BRANCH_MANAGER
Branch ID: not provided
```

Since the branch ID is absent, your manager availability check is skipped.

Later, this code runs:

```
savedUser.getBranch().setManager(savedUser);
```

That could replace Amit as the branch's manager without checking whether Amit was already assigned.

Fix: Determine the employee's effective destination branch first. Then validate manager availability whenever the resulting role is `ROLE_BRANCH_MANAGER`, whether or not the client supplied a new branch ID.

Also enforce that a branch-scoped role must have a branch, and a store-level role must follow your intended store-level assignment rules.

### 4. Validate email uniqueness and request data

Your visible service methods don't check for an existing employee with the same email before saving.

Add an appropriate repository check, for example:

```
if (userRepository.findByEmail(employee.getEmail()).isPresent()) {
    throw new BadRequestException("Email already exists");
}
```

Adjust the code to match the actual return type of your repository method.

Also:

- Add `@Valid` to controller request bodies.
- Validate name, email, password, phone and role.
- Avoid accidentally setting existing fields to `null` when a client omits them from an update request.

### 5. Separate request DTOs from response DTOs

Currently, you're using `UserDto` for both incoming employee data and outgoing responses.

This deserves attention because the request may contain a password.

A cleaner design would be:

```
CreateEmployeeRequest
UpdateEmployeeRequest
EmployeeResponse
```

The response should never contain the password, whether raw or encoded.

### 6. Avoid unnecessary database queries

There are a couple of opportunities for improvement:

In `updateEmployee()`, you fetch the destination branch using `findByIdForUpdate()` and then fetch it again using `findById()`. Reuse the locked `Branch` object.

In `findBranchEmployees()`, you call `requireBranchAccess()` and then call `branchRepository.findById()` again. Depending on the implementation of `requireBranchAccess()`, this may fetch the same branch twice.

Finally, move role filtering into repository queries for large employee lists rather than loading everything and filtering in Java.

### 7. Make authorization failures return the correct status

Your earlier `GlobalExceptionHandler` maps `UserException` to HTTP 400.

But the distinction should generally be:

| Situation                                  | HTTP status |
| ------------------------------------------ | ----------- |
| Employee does not exist                    | 404         |
| Invalid role or request data               | 400         |
| User is authenticated but lacks permission | 403         |
| User is not authenticated                  | 401         |
| Unexpected server failure                  | 500         |

Use appropriate exceptions for these situations instead of throwing generic `RuntimeException` everywhere.

## 5. Complete module data flow

Here is how the employee module fits into the rest of your POS application.

Creating an employee

EmployeeController

Receives HTTP request and request DTO

EmployeeServiceImpl

Business rules, authorization and validation

UserRepository

Save employee

StoreRepository

Find store

BranchRepository

Assign branch

Database

Users, stores, branches and their relationships

## 6. Interview questions from this module

These are the questions I'd prepare based on your actual implementation.

Q1. Why are you using UserRepository instead of EmployeeRepository?

Because employees are represented by the `User` entity. Their employee type is determined by their role and their store/branch relationships.

Q2. Why do you use `@Transactional` in employee creation?

Because creating a branch manager involves saving the user and updating the branch's manager reference. Both operations should succeed or fail together.

Q3. What is pessimistic locking?

It locks a database row so that another transaction cannot concurrently modify that locked row. Here, it helps prevent two requests from assigning different managers to the same branch.

Q4. Why do you check authorization against both the previous and target branch during an update?

Because moving an employee affects two resources. The user must have permission to manage the employee's current location and the destination location.

Q5. Why do you encode passwords using `PasswordEncoder`?

To store a secure password hash instead of storing the original password. During login, Spring Security can verify the submitted password against that hash.

Q6. Why do you clear the branch manager reference before deleting a branch manager?

To avoid leaving a branch referencing a user who is being deleted, which could also cause database relationship or foreign-key problems.

Q7. What is the purpose of `@CacheEvict`?

It invalidates cached data after changes so that later requests don't return outdated user or branch information.

## 7. What you should remember

| Concept           | What your implementation does               |
| ----------------- | ------------------------------------------- |
| Employee creation | Creates a `User` entity                     |
| Store assignment  | Sets `user.store`                           |
| Branch assignment | Sets `user.branch`                          |
| Branch manager    | Also sets `branch.manager`                  |
| Authentication    | Gets the current user through `UserService` |
| Authorization     | Uses `AccessControlService`                 |
| Password          | Encodes with `PasswordEncoder`              |
| Atomic operations | Uses `@Transactional`                       |
| Cache consistency | Uses `@CacheEvict`                          |
| DTO conversion    | Uses `UserMapper`                           |

My assessment: The architecture is sensible and you're handling some important real-world cases, especially employee transfers, manager replacement and permission checks. I would prioritize fixing the role validation, store/branch ownership check and manager-update edge case before adding more POS functionality.

The next logical module is Categories and Products, followed by Branch Inventory. Those modules will build on the store, branch and employee relationships you've established here.