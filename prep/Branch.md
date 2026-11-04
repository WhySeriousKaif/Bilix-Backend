# Part 5: Branch Management Flow in Spring Boot

Now we're moving from Store Management to Branch Management.

Your architecture is consistent with the previous module:

```
BranchController
      ↓
BranchService (interface)
      ↓
BranchServiceImp
      ↓
BranchRepository
      ↓
Branch Entity
      ↓
BranchMapper
      ↓
BranchDto
      ↓
Frontend
```

The main difference is that a store can have multiple branches.

For example:

Kaif Mart — Store ID 101

Branch 1 — Bengaluru

Branch ID: 1 · Manager: Employee A

Branch 2 — Kolkata

Branch ID: 2 · Manager: Employee B

Branch 3 — Delhi

Branch ID: 3 · Manager: Employee C

A Store Admin can manage branches belonging to their store. A Branch Manager should be restricted to their assigned branch. A Super Admin can manage the entire system.

Your `AccessControlService` is used to enforce these restrictions.

## 1. BranchController — API layer

```
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
}
```

These annotations should be familiar now:

- `@RestController`: Exposes REST APIs.
- `@RequestMapping`: Common URL prefix.
- `@RequiredArgsConstructor`: Generates constructor injection.
- `BranchService`: Handles branch business logic.

### Your API endpoints

| HTTP method | Endpoint                        | Purpose                     |
| ----------- | ------------------------------- | --------------------------- |
| POST        | `/api/branches`                 | Create branch               |
| GET         | `/api/branches`                 | Get all accessible branches |
| GET         | `/api/branches/{id}`            | Get branch by ID            |
| GET         | `/api/branches/store/{storeId}` | Get branches of a store     |
| PUT         | `/api/branches/{id}`            | Update branch               |
| DELETE      | `/api/branches/{id}`            | Delete branch               |

You pasted the `BranchController` three times, but it is the same code. You only need one copy in your project.

### Example: Create branch

```
@PostMapping
public ResponseEntity<BranchDto> createBranch(
        @RequestBody BranchDto branchDto) {

    return ResponseEntity.ok(
            branchService.createBranch(branchDto)
    );
}
```

Suppose the frontend sends:

```
{
  "name": "Bengaluru Main Branch",
  "phone": "9876543210",
  "address": "Bengaluru",
  "email": "bangalore@kaifmart.com",
  "workingDays": ["MONDAY", "TUESDAY", "WEDNESDAY"],
  "openTime": "09:00:00",
  "closeTime": "21:00:00"
}
```

Spring converts this JSON into a `BranchDto` using Jackson.

Then the controller passes it to:

```
branchService.createBranch(branchDto);
```

Notice something important: the frontend does not provide the store ID in this example.

Your service determines the store using the authenticated user. That's a useful security design because the client cannot simply choose another store's ID.

One improvement: use HTTP 201 Created for a successful branch creation instead of HTTP 200.

## 2. BranchService — interface

```
public interface BranchService {

    BranchDto createBranch(BranchDto branchDto);

    BranchDto getBranchById(Long id);

    List<BranchDto> getBranchesByStoreId(Long storeId);

    List<BranchDto> getAllBranches();

    BranchDto updateBranch(Long id, BranchDto branchDto);

    void deleteBranch(Long id);
}
```

This interface defines the available operations.

The actual implementation is in `BranchServiceImp`.

This is the same interface-implementation pattern you used for StoreService and UserService.

## 3. BranchServiceImp — the main business logic

```
@Service
@RequiredArgsConstructor
public class BranchServiceImp implements BranchService {

    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;
}
```

Let's understand these four dependencies.

| Dependency           | Responsibility                            |
| -------------------- | ----------------------------------------- |
| BranchRepository     | Database operations for branches          |
| StoreRepository      | Finds the store to which a branch belongs |
| UserService          | Gets the currently logged-in user         |
| AccessControlService | Checks whether that user has permission   |

This is a good separation of responsibilities.

## 4. `createBranch()` — complete walkthrough

Your implementation:

```
@Override
@CacheEvict(
    cacheNames = {"branches", "branchesByStore"},
    allEntries = true
)
public BranchDto createBranch(BranchDto branchDto) {

    User currentUser = userService.getCurrentUser();

    Store store = storeRepository.findByStoreAdminId(
            currentUser.getId()
    );

    if (store == null) {
        throw new NotFoundException(
            "Only a Store Admin can create a branch for their store"
        );
    }

    Branch branch = BranchMapper.toEntity(branchDto, store);

    Branch savedBranch = branchRepository.save(branch);

    return BranchMapper.toDto(savedBranch);
}
```

Let's understand the flow.

Step 1: Get the logged-in user

```
User currentUser = userService.getCurrentUser();
```

This uses `SecurityContextHolder` indirectly through your `UserService`.

For example, the Store Admin has logged in:

```
Email: admin@kaifmart.com
User ID: 15
Role: ROLE_STORE_ADMIN
```

Step 2: Find the admin's store

```
Store store = storeRepository.findByStoreAdminId(
        currentUser.getId()
);
```

This searches for the store whose `storeAdmin` relationship points to User ID 15.

Conceptually:

```
SELECT *
FROM store
WHERE store_admin_id = 15;
```

If no store exists, your code throws an exception.

Step 3: Convert DTO into Entity

```
Branch branch = BranchMapper.toEntity(branchDto, store);
```

This is important.

The incoming DTO contains branch details, but your mapper receives the store separately.

Therefore, the store relationship comes from the backend, not from an arbitrary store ID supplied by the frontend.

Step 4: Save the branch

```
Branch savedBranch = branchRepository.save(branch);
```

Spring Data JPA saves the new branch.

Conceptually, your database gets:

| id | name                  | store_id |
| -- | --------------------- | -------- |
| 1  | Bengaluru Main Branch | 101      |

The `store_id` foreign key connects this branch to Store 101.

Step 5: Return DTO

```
return BranchMapper.toDto(savedBranch);
```

The saved entity is converted into a DTO and returned to the frontend.

### Complete flow

```
Frontend sends BranchDto
          |
          v
BranchController
          |
          v
BranchServiceImp
          |
          v
Get currently logged-in User
          |
          v
Find Store belonging to User
          |
          v
BranchMapper.toEntity()
          |
          v
BranchRepository.save()
          |
          v
BranchMapper.toDto()
          |
          v
Frontend receives BranchDto
```

One important observation: Your code effectively allows only a user who owns a store through `storeAdmin` to create branches. But it doesn't explicitly check `ROLE_STORE_ADMIN`. I'd add that role check so the business rule is clear rather than relying only on the repository lookup.

## 5. `getBranchById()` — retrieving one branch

```
@Override
@Cacheable(cacheNames = "branches", key = "#id")
public BranchDto getBranchById(Long id) {

    Branch branch = branchRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Branch not found with id: " + id
                )
            );

    accessControlService.requireBranchAccess(
            userService.getCurrentUser(),
            id
    );

    return BranchMapper.toDto(branch);
}
```

This method performs three operations:

1. Fetch the branch by ID.
2. Check whether the current user has access to it.
3. Convert the branch into a DTO.

The access check is particularly important.

Suppose:

- Store A has Branch 1.
- Store B has Branch 2.
- A cashier belongs to Branch 1.

If that cashier requests:

```
GET /api/branches/2
```

Your `requireBranchAccess()` checks whether the user is assigned to Branch 2 or has access through their store.

If not, it throws an exception.

### Important production issue: caching and authorization

There is a serious issue with the order of operations here.

You have:

```
@Cacheable(cacheNames = "branches", key = "#id")
```

Spring's caching interceptor can return a cached result without executing the method body.

That means the first request might populate the cache, and a later request from another user might receive that cached result without running `requireBranchAccess()`.

Authorization must never be skipped because of a cache hit.

A safer approach is to perform authorization outside the cached data lookup, or make the cache key and caching design account for the authorization boundary. For example:

```
public BranchDto getBranchById(Long id) {
    Branch branch = branchRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Branch not found")
            );

    accessControlService.requireBranchAccess(
            userService.getCurrentUser(), id
    );

    return getCachedBranchDto(id);
}

@Cacheable(cacheNames = "branches", key = "#id")
public BranchDto getCachedBranchDto(Long id) {
    Branch branch = branchRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Branch not found")
            );

    return BranchMapper.toDto(branch);
}
```

The cached method should be invoked through a separate Spring bean (or otherwise through the Spring proxy); self-invocation within the same class won't trigger proxy-based caching.

This is a security-sensitive fix, not just a performance optimization.

## 6. `getBranchesByStoreId()` — get all branches of a store

```
@Override
@Cacheable(cacheNames = "branchesByStore", key = "#storeId")
public List<BranchDto> getBranchesByStoreId(Long storeId) {

    accessControlService.requireStoreAccess(
            userService.getCurrentUser(),
            storeId
    );

    List<Branch> branches =
            branchRepository.findByStoreId(storeId);

    return branches.stream()
            .map(BranchMapper::toDto)
            .collect(Collectors.toList());
}
```

Let's understand this.

Suppose Store 101 has three branches.

The frontend calls:

```
GET /api/branches/store/101
```

Your service first checks:

```
accessControlService.requireStoreAccess(
        userService.getCurrentUser(),
        storeId
);
```

This verifies that the logged-in user has access to Store 101.

Then:

```
branchRepository.findByStoreId(storeId);
```

Fetches all branches belonging to that store.

Your repository doesn't explicitly define this method in the code you shared earlier, so you'll need this declaration:

```
List<Branch> findByStoreId(Long storeId);
```

Spring Data JPA understands the relationship and generates the corresponding query.

Finally, your stream converts every entity into a DTO.

Another important caching issue: This method has the same authorization-before-cache problem. On a cache hit, the access check may be skipped. The same solution applies.

## 7. `getAllBranches()` — Super Admin vs Store Admin

```
@Override
public List<BranchDto> getAllBranches() {

    User user = userService.getCurrentUser();

    if (user.getRole().equals(UserRole.ROLE_ADMIN)) {

        return branchRepository.findAll().stream()
                .map(BranchMapper::toDto)
                .collect(Collectors.toList());

    } else {

        Store store = storeRepository.findByStoreAdminId(
                user.getId()
        );

        if (store == null) {
            throw new NotFoundException(
                "Store not found for this admin. Please create a store first."
            );
        }

        return getBranchesByStoreId(store.getId());
    }
}
```

Your intended business rule is:

| User           | Expected access         |
| -------------- | ----------------------- |
| Super Admin    | All branches            |
| Store Admin    | Branches of their store |
| Branch Manager | Their branch            |
| Cashier        | Their branch            |

But your current implementation has a mismatch.

Every non-admin user goes through:

```
storeRepository.findByStoreAdminId(user.getId());
```

That only finds a store where the user is the registered Store Admin.

A Branch Manager or Cashier won't normally have that relationship.

So they will receive a "Store not found" error even if they have a valid branch assignment.

I'd explicitly define the policy:

- Super Admin: all branches.
- Store Admin/Store Manager: all branches of their store.
- Branch Manager/Cashier: their assigned branch only, if this endpoint is intended to support them.
- Other roles: denied.

This will make the implementation consistent with your access-control design.

## 8. `updateBranch()` — updating branch information

Your implementation:

```
@Override
@CacheEvict(
    cacheNames = {"branches", "branchesByStore"},
    allEntries = true
)
public BranchDto updateBranch(Long id, BranchDto branchDto) {

    Branch existingBranch = branchRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Branch not found with id: " + id
                )
            );

    accessControlService.requireBranchInfoEditAccess(
            userService.getCurrentUser(),
            existingBranch
    );

    if (branchDto.getName() != null)
        existingBranch.setName(branchDto.getName());

    if (branchDto.getPhone() != null)
        existingBranch.setPhone(branchDto.getPhone());

    if (branchDto.getAddress() != null)
        existingBranch.setAddress(branchDto.getAddress());

    if (branchDto.getEmail() != null)
        existingBranch.setEmail(branchDto.getEmail());

    if (branchDto.getWorkingDays() != null)
        existingBranch.setWorkingDays(branchDto.getWorkingDays());

    if (branchDto.getOpenTime() != null)
        existingBranch.setOpenTime(branchDto.getOpenTime());

    if (branchDto.getCloseTime() != null)
        existingBranch.setCloseTime(branchDto.getCloseTime());

    Branch savedBranch = branchRepository.save(existingBranch);

    return BranchMapper.toDto(savedBranch);
}
```

This is a partial-update approach.

For example, the frontend sends:

```
{
  "name": "Bengaluru Updated Branch"
}
```

Only the branch name changes.

Why?

Because you check:

```
if (branchDto.getName() != null)
```

If the frontend doesn't send the phone number, it remains unchanged.

This is better than replacing every field with potentially null values.

### What is `requireBranchInfoEditAccess()` doing?

It checks whether the user can edit the branch's contact and display information.

Your earlier `AccessControlService` permits a user assigned to the branch to update this information, including a cashier.

This is intentionally less restrictive than employee-management or branch-deletion permission.

One caveat: because you're using `PUT`, but implementing partial-update behavior, your API semantics are a little inconsistent. A `PATCH /api/branches/{id}` endpoint would better communicate partial updates.

## 9. `deleteBranch()` — deleting a branch

```
@Override
@CacheEvict(
    cacheNames = {"branches", "branchesByStore"},
    allEntries = true
)
public void deleteBranch(Long id) {

    Branch branch = branchRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Branch not found with id: " + id
                )
            );

    accessControlService.requireManagerAccessToBranch(
            userService.getCurrentUser(),
            branch
    );

    branchRepository.delete(branch);
}
```

This method is a good example of separating two different checks:

1. Does the branch exist?
2. Is the current user allowed to delete it?

Your `requireManagerAccessToBranch()` allows:

- Super Admin.
- Store Admin of the branch's store.
- Store Manager of the branch's store.
- Branch Manager assigned to that branch.

It denies ordinary cashiers.

That's a sensible permission distinction.

However, before physically deleting a branch in a POS system, you should consider its orders, inventory, employees, shifts and reports. Historical business records should generally be preserved.

## 10. BranchRepository — database layer

Your repository:

```
public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    List<Branch> findByStoreId(Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Branch b where b.id = :id")
    Optional<Branch> findByIdForUpdate(@Param("id") Long id);
}
```

You have two methods worth understanding.

### A. `findByStoreId()`

```
List<Branch> findByStoreId(Long storeId);
```

Spring Data JPA generates a query based on the method name.

Conceptually:

```
SELECT *
FROM branch
WHERE store_id = ?;
```

It returns all branches belonging to the specified store.

### B. `findByIdForUpdate()`

This is more advanced.

```
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select b from Branch b where b.id = :id")
Optional<Branch> findByIdForUpdate(
        @Param("id") Long id
);
```

What is pessimistic locking?

Imagine two administrators attempt to update the same branch simultaneously.

Without locking:

1. Admin A reads the branch.
2. Admin B reads the same branch.
3. Admin A updates it.
4. Admin B updates it using the older data.
5. Admin A's changes might be overwritten.

A pessimistic write lock asks the database to lock the selected row while the transaction is working with it.

Conceptually:

```
SELECT *
FROM branch
WHERE id = 1
FOR UPDATE;
```

The second transaction may have to wait until the first transaction completes.

Important: Your repository defines `findByIdForUpdate()`, but your current `BranchServiceImp` doesn't call it.

Also, this method must be used inside an appropriate transaction for the lock to remain effective during the operation.

You don't need pessimistic locking for every database read. Use it for operations where concurrent modifications could cause serious consistency problems.

## 11. Branch Entity — understanding your database model

This is where the actual branch data is represented.

```
@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Branch {
```

- `@Entity`: Marks this class as a JPA entity.
- `@AllArgsConstructor`: Lombok generates a constructor with all fields.
- `@Getter` and `@Setter`: Generates accessors.
- `@NoArgsConstructor`: Generates a no-argument constructor, which JPA requires.
- `@Builder`: Enables the builder pattern.

### A. Primary key

```
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

`@Id` identifies the primary key.

`@GeneratedValue` tells JPA that the ID should be generated automatically.

`GenerationType.AUTO` lets the JPA provider choose an appropriate generation strategy for the database.

### B. Ordinary fields

```
private String name;
private String phone;
private String address;
private String email;
```

These represent the branch's basic information.

JPA maps these fields to columns in the branch table.

### C. `@ElementCollection`

```
@ElementCollection
private List<String> workingDays;
```

This is an interesting concept.

You have a list of working days:

```
List<String> workingDays;
```

For example:

```
List.of(
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY"
);
```

Why use `@ElementCollection`?

Because a normal relational database column cannot directly store a Java `List<String>` as separate relational values.

JPA creates a separate collection table.

Conceptually:

Branch table

| id | name             |
| -- | ---------------- |
| 1  | Bengaluru Branch |

Branch working days table

| branch_id | working_days |
| --------- | ------------ |
| 1         | MONDAY       |
| 1         | TUESDAY      |
| 1         | WEDNESDAY    |
| 1         | THURSDAY     |

The `branch_id` connects the working days to the branch.

You don't need to create a separate WorkingDay entity because these are simple values, not independent business objects.

### D. LocalTime

```
private LocalTime openTime;
private LocalTime closeTime;
```

`LocalTime` stores time without a date or timezone.

Example:

```
LocalTime open = LocalTime.of(9, 0);
LocalTime close = LocalTime.of(21, 0);
```

This is appropriate for regular store opening and closing times.

### E. Store relationship — Many-to-One

```
@ManyToOne
private Store store;
```

This is one of the most important relationships in your POS application.

It means:

Many branches can belong to one store.

For example:

```
              STORE
             ID: 101
                |
       -------------------
       |        |        |
       v        v        v
    Branch 1 Branch 2 Branch 3
```

In the database, the branch table will contain a foreign key pointing to the store.

Conceptually:

```
branch
-------------------------
id | name       | store_id
1  | Bengaluru  | 101
2  | Kolkata    | 101
3  | Delhi      | 101
```

This is why your `BranchMapper.toEntity()` receives a `Store` object.

It establishes the relationship:

```
branch.setStore(store);
```

### F. Branch manager — One-to-One

```
@OneToOne(cascade = CascadeType.REMOVE)
private User manager;
```

Your comment says one branch has one manager.

`@OneToOne` means one Branch is associated with one User as its manager, and the same manager relationship is unique at the database level when the schema is generated with the appropriate unique constraint.

Conceptually:

```
Branch 1  → Manager A
Branch 2  → Manager B
Branch 3  → Manager C
```

But there is an important problem with your cascade setting.



Important

`CascadeType.REMOVE` means deleting a branch can also delete its associated manager User.



For example:

```
branchRepository.delete(branch);
```

JPA may also delete the User record associated with `branch.manager`.

That is probably not what you want in a POS system.

Deleting a branch should not automatically delete an employee's entire account.

I'd normally change this to:

```
@OneToOne
private User manager;
```

Only retain `CascadeType.REMOVE` if deleting a branch is genuinely supposed to delete the manager's account.

Another relationship to verify: In the User entity you shared earlier, you also had:

```
@ManyToOne
private Branch branch;
```

That is a separate association from `Branch.manager` unless you've explicitly connected them with `mappedBy` and a matching join column. You should check whether you intend to maintain both relationships. Otherwise, you could end up with two different database relationships representing the same manager assignment.&#x20;

### G. Lifecycle callbacks

```
@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

`@PreUpdate` runs before JPA executes an update to the entity.

So whenever a managed Branch is updated and the change is flushed, `updatedAt` gets refreshed.

Similarly:

```
@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
}
```

`@PrePersist` runs before the entity is first inserted.

It automatically sets the creation timestamp.

One improvement: set `updatedAt` during creation too.

```
@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}
```

Now both timestamps are initialized.

### H. equals() and hashCode()

You have:

```
@Override
public boolean equals(Object o) {
    if (this == o)
        return true;

    if (o == null || getClass() != o.getClass())
        return false;

    Branch branch = (Branch) o;

    return id != null && id.equals(branch.id);
}
```

This checks whether two Branch objects represent the same database record.

For example:

```
Branch branch1 = ...; // ID 10
Branch branch2 = ...; // ID 10
```

They are considered equal because they have the same non-null ID.

Your `hashCode()`:

```
@Override
public int hashCode() {
    return getClass().hashCode();
}
```

Uses the entity class as the hash code.

This avoids changing the hash code when a new entity receives its generated ID, which is useful when entities are stored in hash-based collections.

## 12. BranchMapper — Entity and DTO conversion

Your mapper has two methods.

### A. `toDto()`

```
public static BranchDto toDto(Branch branch) {

    return BranchDto.builder()
            .id(branch.getId())
            .name(branch.getName())
            .phone(branch.getPhone())
            .address(branch.getAddress())
            .email(branch.getEmail())
            .workingDays(branch.getWorkingDays())
            .openTime(branch.getOpenTime())
            .closeTime(branch.getCloseTime())
            .createdAt(branch.getCreatedAt())
            .updatedAt(branch.getUpdatedAt())
            .storeId(
                branch.getStore() != null
                    ? branch.getStore().getId()
                    : null
            )
            .manager(
                branch.getManager() != null
                    ? UserMapper.toDto(branch.getManager())
                    : null
            )
            .build();
}
```

This converts a Branch entity into a BranchDto.

Notice this:

```
.storeId(branch.getStore() != null
        ? branch.getStore().getId()
        : null)
```

Your entity contains a complete Store object, but your DTO only needs its ID.

That's a good example of why DTOs are useful.

And:

```
.manager(
    branch.getManager() != null
        ? UserMapper.toDto(branch.getManager())
        : null
)
```

The mapper converts the manager entity into a UserDto.

This avoids exposing the complete User entity.

### B. `toEntity()`

```
public static Branch toEntity(
        BranchDto branchDto,
        Store store) {

    return Branch.builder()
            .id(branchDto.getId())
            .name(branchDto.getName())
            .phone(branchDto.getPhone())
            .address(branchDto.getAddress())
            .store(store)
            .email(branchDto.getEmail())
            .workingDays(branchDto.getWorkingDays())
            .openTime(branchDto.getOpenTime())
            .closeTime(branchDto.getCloseTime())
            .createdAt(branchDto.getCreatedAt())
            .updatedAt(branchDto.getUpdatedAt())
            .build();
}
```

This performs the reverse conversion.

Notice:

```
.store(store)
```

The store is passed from the service rather than taken from the frontend.

That's a good security decision.

However, I would change three things:

1. Don't set the entity ID from the incoming DTO when creating a new branch.
2. Don't accept creation or update timestamps from the frontend.
3. Don't allow the frontend to assign the manager directly through this mapper. Manager assignment should have its own authorized operation.

A cleaner creation mapper would look like:

```
public static Branch toEntity(BranchDto dto, Store store) {

    return Branch.builder()
            .name(dto.getName())
            .phone(dto.getPhone())
            .address(dto.getAddress())
            .email(dto.getEmail())
            .workingDays(dto.getWorkingDays())
            .openTime(dto.getOpenTime())
            .closeTime(dto.getCloseTime())
            .store(store)
            .build();
}
```

Your entity lifecycle callbacks can manage timestamps.

Also, setting a non-null ID in a create request can make Spring Data JPA treat the entity as existing and call `merge()` instead of `persist()`. That can result in an unintended update rather than a clean insert.

# 13. Important code review — changes I'd make

Your Branch module is reasonably well structured. However, there are a few issues I'd fix before considering it production-ready.

| Priority | Issue                                                          | Recommended change                                        |
| -------- | -------------------------------------------------------------- | --------------------------------------------------------- |
| Critical | `@Cacheable` may bypass authorization checks                   | Move authorization outside cached methods                 |
| Critical | `CascadeType.REMOVE` on manager                                | Remove it unless deleting a branch should delete the user |
| High     | Branch creation doesn't explicitly check role                  | Explicitly require Store Admin                            |
| High     | `getAllBranches()` doesn't support branch-level staff properly | Define role-specific access                               |
| High     | Mapper accepts ID and timestamps                               | Generate these on the backend                             |
| Medium   | `updatedAt` isn't initialized on creation                      | Set it in `@PrePersist`                                   |
| Medium   | Branch creation returns HTTP 200                               | Return HTTP 201                                           |
| Medium   | No `@Valid` on BranchDto                                       | Add validation annotations                                |
| Medium   | Pessimistic lock method isn't used                             | Use it only where concurrent updates require it           |

### A. The caching issue deserves special attention

You have:

```
@Cacheable(cacheNames = "branches", key = "#id")
```

and:

```
@Cacheable(cacheNames = "branchesByStore", key = "#storeId")
```

Suppose Store Admin A accesses Branch 1 first. The result gets cached.

Later, Cashier B requests Branch 1.

If the cache returns the result without executing the method, your access-control check won't run.

This is why you must separate authorization from cached data retrieval.

This is one of the most important fixes in this module.

### B. Your role permissions need to be consistent

Based on the AccessControlService you shared earlier, your intended permissions look like this:

| Operation         | Super Admin   | Store Admin | Store Manager | Branch Manager | Cashier    |
| ----------------- | ------------- | ----------- | ------------- | -------------- | ---------- |
| Create branch     | Decide policy | Yes         | Decide policy | No             | No         |
| View all branches | All           | Own store   | Own store     | No             | No         |
| View own branch   | Yes           | Yes         | Yes           | Yes            | Yes        |
| Edit branch info  | Yes           | Own store   | Own store     | Own branch     | Own branch |
| Delete branch     | Yes           | Own store   | Own store     | Own branch     | No         |

Your current implementation doesn't fully enforce this table. In particular, `getAllBranches()` and `createBranch()` need clearer role checks.

You should define the permissions first, then implement them consistently in `AccessControlService`.

### C. Deleting a branch should not delete its manager

I would remove:

```
cascade = CascadeType.REMOVE
```

from the manager relationship.

Your branch and employee account have different lifecycles.

A branch can close while the employee account still exists and might be reassigned to another branch.

### D. Make branch creation transactional where appropriate

For your current `createBranch()`, you're performing one main database save, so a transaction is already provided by the Spring Data repository operation.

You don't strictly need to add `@Transactional` just for that one save.

But if you later create a branch, assign a manager, create inventory records and initialize other related data together, then put the complete operation in a service-level transaction.

# 14. Complete Branch Management flow

Let's put the pieces together.

## Branch module architecture

BranchController

Receives HTTP requests

BranchService

Defines operations

BranchServiceImp

Business logic and orchestration

AccessControlService

Authorization

UserService

Current user

BranchRepository

Database queries

Branch Entity

JPA database representation

BranchMapper → BranchDto

Converts entity into API response

# 15. Interview revision

These are the concepts from your code that you should be able to explain.

| Interview question            | Answer                                                                                                     |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------- |
| What is `@ManyToOne`?         | Many branch records can reference one store.                                                               |
| What is `@OneToOne`?          | One branch is associated with one manager in this mapping.                                                 |
| What is `@ElementCollection`? | Stores a collection of basic values in a separate collection table.                                        |
| What is `@PrePersist`?        | Runs before a new entity is persisted.                                                                     |
| What is `@PreUpdate`?         | Runs before an entity update is persisted.                                                                 |
| What is pessimistic locking?  | A database locking strategy that prevents competing transactions from modifying a locked row concurrently. |
| What is `CascadeType.REMOVE`? | Propagates entity deletion to associated entities.                                                         |
| What is a derived query?      | A query generated from a Spring Data repository method name.                                               |
| Why use a mapper?             | To convert between entity and DTO representations.                                                         |
| Why use `@CacheEvict`?        | To invalidate stale cached data after changes.                                                             |

## 16. My overall assessment

Good foundation

I like how you've structured your Store and Branch modules. You're applying several useful backend concepts:

- Layered architecture.
- Constructor dependency injection.
- Spring Data JPA.
- DTO and mapper patterns.
- Role-based authorization.
- Resource ownership checks.
- Caching.
- Entity relationships.
- Database locking.

But I would prioritize fixing the cache authorization bypass and manager deletion cascade before moving forward.

What I want you to understand particularly well: A Store Admin owns a store, a store has multiple branches, and employees are assigned to branches. Every request must respect that hierarchy. This is the foundation of multi-tenant POS security.

### Next step

I'd suggest moving to the Employee Management module next. That's where your `AccessControlService`, `Branch`, `Store`, `UserRole`, and employee relationships all come together.

Send your `EmployeeController`, `EmployeeService` and `EmployeeServiceImpl` code, and we'll follow the same step-by-step approach.