# Part 4: Store Management Flow in Spring Boot

Your store module follows the layered architecture we discussed:

```
Controller → Service Interface → Service Implementation
           → Repository → Entity → Mapper → DTO → Response
```

This is a realistic structure for a production-style Spring Boot application.

Your POS application has an interesting business relationship:

- A Store Admin creates a store.
- A store belongs to one Store Admin.
- Employees belong to a store.
- A store can have multiple branches.
- A Super Admin can manage all stores.
- A Store Admin should only manage their own store.

Let's understand how your code implements these operations.

## 1. StoreController — the API layer

Your controller starts with:

```
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;
}
```

### What is happening here?

- `@RestController`: This class exposes REST APIs.
- `@RequestMapping("/api/stores")`: All endpoints share this URL prefix.
- `@RequiredArgsConstructor`: Lombok generates a constructor for the two final fields.
- `StoreService`: Handles store-related business operations.
- `UserService`: Retrieves the currently authenticated user or identifies a user from a JWT.

Notice that your controller does not directly interact with `StoreRepository`.

That's good separation of responsibilities.

### Your available endpoints

| HTTP method | Endpoint                    | Purpose                             |
| ----------- | --------------------------- | ----------------------------------- |
| POST        | `/api/stores`               | Create a store                      |
| GET         | `/api/stores/{id}`          | Get a store by ID                   |
| GET         | `/api/stores`               | Get all stores                      |
| GET         | `/api/stores/admin`         | Get stores visible to an admin      |
| GET         | `/api/stores/employee`      | Get the logged-in employee's store  |
| GET         | `/api/stores/my`            | Get the logged-in admin's own store |
| GET         | `/api/stores/my/employees`  | Get employees in the admin's store  |
| PUT         | `/api/stores/{id}`          | Update a store                      |
| DELETE      | `/api/stores/{id}`          | Delete a store                      |
| PUT         | `/api/stores/{id}/moderate` | Change store status                 |

## 2. Understanding the create-store flow

This is the most important operation to understand first.

Your controller:

```
@PostMapping
public ResponseEntity<StoreDto> createStore(
        @RequestBody StoreDto storeDto,
        @RequestHeader("Authorization") String jwt)
        throws UserException {

    User user = userService.getUserFromJwt(jwt);

    return ResponseEntity.ok(
            storeService.createStore(storeDto, user)
    );
}
```

Let's understand it step by step.

Step 1: Frontend sends a request

For example:

```
POST /api/stores
Authorization: Bearer <JWT>
Content-Type: application/json
```

Request body:

```
{
  "brand": "Kaif Mart",
  "description": "A retail grocery store",
  "storeType": "GROCERY"
}
```

Step 2: Jackson converts JSON into StoreDto

Because of:

```
@RequestBody StoreDto storeDto
```

Spring uses Jackson to convert the incoming JSON into a Java object.

Conceptually:

```
StoreDto storeDto = new StoreDto();
storeDto.setBrand("Kaif Mart");
storeDto.setDescription("A retail grocery store");
```

You don't manually create this object. Spring does it.

Step 3: Identify the logged-in user

```
User user = userService.getUserFromJwt(jwt);
```

Your `UserServiceImp`:

1. Removes the `Bearer ` prefix.
2. Extracts the email from the JWT.
3. Finds the corresponding user in the database.
4. Returns the User entity.

For example:

```
JWT
 |
 v
Extract email: kaif@gmail.com
 |
 v
Find user in database
 |
 v
User entity
```

Step 4: Call the service

```
storeService.createStore(storeDto, user);
```

Now the controller passes two things:

- `storeDto`: What store should be created?
- `user`: Who is requesting the creation?

This is useful because the service needs to verify the user's role and associate the store with the correct admin.

## 3. StoreService — the interface

Your interface:

```
public interface StoreService {

    StoreDto createStore(StoreDto storeDto, User user);

    StoreDto getStoreById(Long id);

    List<StoreDto> getAllStores();

    StoreDto getStoreByAdmin(User user);

    List<StoreDto> getStoresForAdmin(User user);

    StoreDto updateStore(Long id, StoreDto storeDto);

    void deleteStore(Long id);

    StoreDto getStoreByEmployee();

    StoreDto moderateStore(Long id, StoreStatus storeStatus);

    StoreDto getMyStore(User user);

    List<UserDto> getMyStoreEmployees(User user)
            throws UserException;
}
```

Remember: an interface defines the contract, not the actual implementation.

For example:

```
StoreDto createStore(StoreDto storeDto, User user);
```

This tells us that the operation:

- Receives a StoreDto.
- Receives the User who is creating the store.
- Returns a StoreDto.

The actual work happens inside `StoreServiceImp`.

One design observation: your interface has both `getStoreByAdmin()` and `getStoresForAdmin()`.

They serve slightly different purposes:

- `getStoreByAdmin()` returns one store.
- `getStoresForAdmin()` returns a list of stores visible to the requesting admin.

That's reasonable, although the naming could be made more explicit.

## 4. StoreServiceImp — the actual business logic

This is where most of your store functionality lives.

```
@Service
@RequiredArgsConstructor
public class StoreServiceImp implements StoreService {

    private final StoreRepository storeRepository;
    private final UserService userService;
    private final EmployeeService employeeService;
}
```

Let's understand the dependencies.

| Dependency      | Why it's needed                         |
| --------------- | --------------------------------------- |
| StoreRepository | Database operations on stores           |
| UserService     | Retrieve and update users               |
| EmployeeService | Retrieve employees belonging to a store |

Notice something important: `StoreServiceImp` doesn't directly implement SQL queries.

It delegates database operations to `StoreRepository`.

### A. `createStore()` — complete walkthrough

```
@Override
@CacheEvict(
    cacheNames = {"stores", "storesAll", "storesByAdmin"},
    allEntries = true
)
public StoreDto createStore(StoreDto storeDto, User user) {
```

`@Override` means this method implements a method declared in `StoreService`.

`@CacheEvict` clears the specified caches when the method successfully completes.

Why?

Suppose your application already cached the list of stores. If a new store is created, the old cached list becomes outdated.

Clearing the cache ensures the next read fetches updated data.

Step 1: Role authorization

```
if (!user.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
    throw new BadRequestException(
        "Only a Store Admin can create a store"
    );
}
```

Only Store Admins can create stores through this method.

Example:

| User role          | Can create store?            |
| ------------------ | ---------------------------- |
| ROLE_ADMIN         | No, according to this method |
| ROLE_STORE_ADMIN   | Yes                          |
| ROLE_STORE_MANAGER | No                           |
| ROLE_CASHIER       | No                           |

Notice that even a Super Admin cannot create a store through this particular method.

That may be intentional, but if Super Admins should also create stores, you'll need to change the rule.

Step 2: Check whether the user already owns a store

```
Store existingStore =
        storeRepository.findByStoreAdminId(user.getId());

if (existingStore != null) {
    throw new BadRequestException(
        "User already has a store. One user can only have one store."
    );
}
```

This is interesting because you're using a custom repository method.

Let's understand it.

Your repository:

```
public interface StoreRepository
        extends JpaRepository<Store, Long> {

    Store findByStoreAdminId(Long adminId);
}
```

Spring Data JPA automatically generates the query based on the method name.

It understands:

- `findBy`: Find a record.
- `StoreAdmin`: Navigate to the `storeAdmin` relationship.
- `Id`: Access the ID of that related user.

Conceptually, it performs something like:

```
SELECT *
FROM stores
WHERE store_admin_id = ?;
```

The exact SQL depends on your entity mapping and database naming strategy.

If a store already exists for this admin, registration is rejected.

Step 3: Convert DTO into Entity

```
Store store = StoreMapper.toEntity(storeDto, user);
```

This is where your Mapper becomes useful.

The frontend sends a `StoreDto`, but JPA saves a `Store` entity.

So you convert:

```
StoreDto → Store Entity
```

We'll examine the mapper in detail shortly.

Step 4: Save the store

```
Store savedStore = storeRepository.save(store);
```

This is the database operation.

Because this is a new entity, JPA generally performs an INSERT.

For example, conceptually:

```
INSERT INTO stores (
    brand,
    description,
    store_admin_id,
    store_type
)
VALUES (
    'Kaif Mart',
    'A retail grocery store',
    15,
    'GROCERY'
);
```

The saved entity now contains its generated database ID.

For example:

```
Store ID: 101
Brand: Kaif Mart
Store Admin ID: 15
```

Step 5: Connect the store back to the user

```
user.setStore(savedStore);

userService.updateUser(user);
```

This is an important relationship.

You are updating the User entity to point to the newly created Store.

Imagine:

```
USER TABLE

id    email             store_id
15    kaif@gmail.com    101
```

And:

```
STORE TABLE

id     brand       store_admin_id
101    Kaif Mart   15
```

The exact foreign-key structure depends on your `User` and `Store` entity mappings.

Why update the user?

Because now you can easily retrieve the logged-in user's store using:

```
user.getStore();
```

This is useful for employee operations, branch creation, inventory, etc.

Step 6: Convert Entity back to DTO

```
return StoreMapper.toDTO(savedStore);
```

The response is a DTO, not a JPA entity.

This is the reverse conversion:

```
Store Entity → StoreDto → JSON → Frontend
```

That's your complete create-store flow.

## Create store: complete data flow

Frontend sends StoreDto

StoreController

Identifies the logged-in user

StoreServiceImp

Checks role and existing store

StoreMapper

DTO → Entity

StoreRepository.save()

INSERT into database

Update User's store relationship

StoreMapper

Entity → DTO

Frontend receives StoreDto

### B. `getStoreById()`

```
@Override
@Cacheable(cacheNames = "stores", key = "#id")
public StoreDto getStoreById(Long id) {

    Store store = storeRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Store not found with id: " + id
                )
            );

    return StoreMapper.toDTO(store);
}
```

This method uses three concepts.

1\. `findById()`

Provided by `JpaRepository`.

It returns:

```
Optional<Store>
```

Why Optional?

Because the requested store may or may not exist.

2\. `orElseThrow()`

If the store doesn't exist, throw `NotFoundException`.

3\. `@Cacheable`

If the store with this ID is already cached, Spring can return the cached DTO without querying the database.

For example:

```
First request: /api/stores/101
       |
       v
Database query
       |
       v
Cache result under stores::101

Second request: /api/stores/101
       |
       v
Return cached result
```

### C. `getAllStores()`

```
@Override
@Cacheable(cacheNames = "storesAll")
public List<StoreDto> getAllStores() {

    List<Store> dtos = storeRepository.findAll();

    return dtos.stream()
            .map(StoreMapper::toDTO)
            .collect(Collectors.toList());
}
```

This fetches all stores.

Let's understand the stream:

```
dtos.stream()
```

Creates a stream from the list.

```
.map(StoreMapper::toDTO)
```

Converts every Store entity into a StoreDto.

```
.collect(Collectors.toList())
```

Collects the converted objects into a new list.

For example:

```
List<Store>
    |
    v
Store 1 → StoreDto 1
Store 2 → StoreDto 2
Store 3 → StoreDto 3
    |
    v
List<StoreDto>
```

In newer Java versions, you can also write:

```
return storeRepository.findAll()
        .stream()
        .map(StoreMapper::toDTO)
        .toList();
```

Both approaches are fine.

### D. `getStoreByAdmin()`

```
@Override
@Cacheable(cacheNames = "storesByAdmin", key = "#user.id")
public StoreDto getStoreByAdmin(User user) {

    Store store = storeRepository.findByStoreAdminId(user.getId());

    if (store == null) {
        throw new NotFoundException(
            "Store not found for this admin. Please create a store first."
        );
    }

    return StoreMapper.toDTO(store);
}
```

This fetches the store owned by a particular Store Admin.

The cache key is the admin's user ID.

For example:

```
storesByAdmin::15 → Store 101
storesByAdmin::20 → Store 205
```

This is useful because different admins should receive different stores.

### E. `getStoresForAdmin()`

```
@Override
public List<StoreDto> getStoresForAdmin(User user) {

    if (user.getRole().equals(UserRole.ROLE_ADMIN)) {
        return getAllStores();
    }

    return List.of(getStoreByAdmin(user));
}
```

This implements two different behaviors:

- Super Admin gets all stores.
- Other users get the store registered under their user ID.

The method name is a little misleading because it also supports Super Admin.

More importantly, it currently treats every non-Super-Admin role as a Store Admin.

I'd explicitly check:

```
if (user.getRole() == UserRole.ROLE_ADMIN) {
    return getAllStores();
}

if (user.getRole() == UserRole.ROLE_STORE_ADMIN) {
    return List.of(getStoreByAdmin(user));
}

throw new BadRequestException("Insufficient permissions");
```

That prevents an unintended role from using this method.

### F. `updateStore()`

This is another important method because it contains authorization.

```
User currentUser = userService.getCurrentUser();
```

You retrieve the currently authenticated user from Spring Security's `SecurityContextHolder`.

Then:

```
Store existingStore = storeRepository.findById(id)
        .orElseThrow(() ->
            new NotFoundException("Store not found with id: " + id)
        );
```

You fetch the store to be updated.

Now comes authorization:

```
if (currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
    // Super admin: allow updating any store
}
else if (currentUser.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {

    Store userStore =
        storeRepository.findByStoreAdminId(currentUser.getId());

    if (userStore == null || !userStore.getId().equals(id)) {
        throw new BadRequestException(
            "You can only update your own store"
        );
    }

} else {
    throw new BadRequestException(
        "Only Super Admin or Store Admin can update stores"
    );
}
```

This is a good ownership check.

For example:

| Current user       | Requested store | Result  |
| ------------------ | --------------- | ------- |
| Super Admin        | Store 101       | Allowed |
| Store Admin of 101 | Store 101       | Allowed |
| Store Admin of 101 | Store 202       | Denied  |
| Cashier            | Store 101       | Denied  |

After authorization, you update the fields:

```
existingStore.setBrand(storeDto.getBrand());
existingStore.setDescription(storeDto.getDescription());
```

For store type:

```
if (storeDto.getStoreType() != null) {
    existingStore.setStoreType(storeDto.getStoreType());
}
```

For contact:

```
if (storeDto.getContact() != null) {

    StoreContact contact = StoreContact.builder()
            .address(storeDto.getContact().getAddress())
            .phone(storeDto.getContact().getPhone())
            .email(storeDto.getContact().getEmail())
            .build();

    existingStore.setContact(contact);
}
```

Here you are creating a new `StoreContact` object instead of directly using the request object.

Finally:

```
return StoreMapper.toDTO(storeRepository.save(existingStore));
```

Save the updated entity and convert it to a DTO.

### G. `deleteStore()`

```
User currentUser = userService.getCurrentUser();

if (!currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
    throw new BadRequestException(
        "Only Super Admin can delete stores"
    );
}
```

Only Super Admin can delete stores.

Then:

```
Store store = storeRepository.findById(id)
        .orElseThrow(() ->
            new NotFoundException("Store not found with id: " + id)
        );

storeRepository.delete(store);
```

This performs a database DELETE operation.

A production POS system should also consider whether the store has branches, orders, inventory, employees or financial records before deleting it. Often, deactivating or archiving a store is safer than physically deleting it.

### H. `getStoreByEmployee()`

```
User currentUser = userService.getCurrentUser();

Store store = currentUser.getStore();

if (store == null) {
    throw new NotFoundException(
        "Store not found for this employee"
    );
}

return StoreMapper.toDTO(store);
```

This doesn't accept a store ID from the frontend.

Instead, it finds the logged-in user and gets their assigned store.

This is a good pattern for a "My Store" feature.

However, there is a potential issue in your data model: some branch employees might have a `branch` assigned but no direct `store` assigned.

Your earlier `AccessControlService` already accounts for that possibility.

You could use the branch's store as a fallback here:

```
Store store = currentUser.getStore();

if (store == null && currentUser.getBranch() != null) {
    store = currentUser.getBranch().getStore();
}
```

Only use this if branch employees are supposed to inherit their store from their branch.

### I. `moderateStore()`

```
User currentUser = userService.getCurrentUser();

if (currentUser == null ||
        !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
    throw new BadRequestException(
        "Only Super Admin can moderate stores"
    );
}
```

This operation is restricted to the Super Admin.

Then:

```
Store store = storeRepository.findById(id)
        .orElseThrow(() ->
            new NotFoundException("Store not found with id: " + id)
        );

store.setStoreStatus(storeStatus);

return StoreMapper.toDTO(storeRepository.save(store));
```

For example, a store might have statuses such as:

- PENDING
- ACTIVE
- SUSPENDED
- REJECTED

The Super Admin can change its status.

The exact allowed statuses depend on your `StoreStatus` enum, which you haven't shared here.

### J. `getMyStore()`

```
@Override
public StoreDto getMyStore(User user) {
    return getStoreByAdmin(user);
}
```

This simply delegates to an existing method.

This is perfectly valid. It avoids duplicating the same database lookup logic.

### K. `getMyStoreEmployees()`

```
Store store = storeRepository.findByStoreAdminId(user.getId());

if (store == null) {
    throw new NotFoundException(
        "Store not found for this admin. Please create a store first."
    );
}

return employeeService.findStoreEmployees(store.getId(), null);
```

The flow:

1. Find the store owned by the admin.
2. Get its ID.
3. Call `EmployeeService`.
4. Retrieve employees belonging to that store.
5. Return the employee DTO list.

Notice that the StoreService doesn't directly query the employee repository.

That's good separation of responsibilities.

## 5. StoreRepository — database communication

Your repository:

```
public interface StoreRepository
        extends JpaRepository<Store, Long> {

    Store findByStoreAdminId(Long adminId);
}
```

You don't need to write an implementation class.

Spring Data JPA generates the implementation automatically at runtime.

Because it extends:

```
JpaRepository<Store, Long>
```

you automatically get methods like:

| Method         | Purpose                 |
| -------------- | ----------------------- |
| `save()`       | Insert or update        |
| `findById()`   | Find by primary key     |
| `findAll()`    | Get all records         |
| `delete()`     | Delete an entity        |
| `existsById()` | Check whether ID exists |
| `count()`      | Count records           |

Your custom method:

```
Store findByStoreAdminId(Long adminId);
```

is a derived query.

One requirement: your `Store` entity must actually have a relationship named `storeAdmin`, and that related entity must have an `id` property.

Also, you pasted `StoreRepository` twice. You only need one declaration in your project.

## 6. StoreMapper — Entity and DTO conversion

Your mapper:

```
public class StoreMapper {
```

This is a normal Java class containing static conversion methods. It isn't currently a Spring bean.

### A. Entity to DTO

```
public static StoreDto toDTO(Store store) {

    StoreDto storeDto = new StoreDto();

    storeDto.setId(store.getId());
    storeDto.setBrand(store.getBrand());
    storeDto.setDescription(store.getDescription());

    if (store.getStoreAdmin() != null) {
        storeDto.setStoreAdmin(
            UserMapper.toDto(store.getStoreAdmin())
        );
    }

    storeDto.setStoreType(store.getStoreType());
    storeDto.setContact(store.getContact());
    storeDto.setCreatedAt(store.getCreatedAt());
    storeDto.setUpdatedAt(store.getUpdatedAt());
    storeDto.setStoreStatus(store.getStoreStatus());

    return storeDto;
}
```

Suppose your database entity looks like this:

```
Store
--------------------------------
id             101
brand          Kaif Mart
description    Grocery Store
storeAdmin     User object
storeType      GROCERY
storeStatus    PENDING
```

Your mapper creates a new StoreDto and copies the required fields.

Why this line?

```
storeDto.setStoreAdmin(
    UserMapper.toDto(store.getStoreAdmin())
);
```

Because `storeAdmin` is a User entity, but your StoreDto expects a UserDto.

So you reuse the UserMapper instead of manually converting the user again.

This is a good approach.

### B. DTO to Entity

```
public static Store toEntity(StoreDto storeDto, User user) {

    Store store = new Store();

    store.setId(storeDto.getId());
    store.setBrand(storeDto.getBrand());
    store.setDescription(storeDto.getDescription());

    store.setStoreAdmin(user);

    store.setStoreType(storeDto.getStoreType());
    store.setContact(storeDto.getContact());

    store.setCreatedAt(storeDto.getCreatedAt());
    store.setUpdatedAt(storeDto.getUpdatedAt());
    store.setStoreStatus(storeDto.getStoreStatus());

    return store;
}
```

This performs the reverse operation.

The important line is:

```
store.setStoreAdmin(user);
```

Notice that the admin is supplied separately rather than being trusted from the frontend's `storeAdmin` field.

That's good because the authenticated user should be the store owner, not an arbitrary user ID provided by the client.

However, there are a few other fields here that should not be trusted from the frontend. We'll discuss that next.

## 7. StoreDto — understanding the data transfer object

Your DTO:

```
@Data
public class StoreDto {

    public Long id;

    public String brand;

    private UserDto storeAdmin;

    private CategoryDto category;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String Description;

    private String storeType;

    private StoreStatus storeStatus;

    private StoreContact contact;
}
```

The purpose of a DTO is to control what information enters and leaves your API.

For example, the frontend doesn't need to receive the entire JPA entity with all its relationships and internal fields.

### Why use `StoreDto` instead of `Store`?

Imagine your Store entity contains:

```
private Long id;
private String brand;
private User storeAdmin;
private List<Branch> branches;
private List<Order> orders;
private LocalDateTime createdAt;
```

Returning this entity directly can create problems:

- Exposing unnecessary information.
- Accidentally exposing sensitive fields through nested entities.
- Lazy-loading issues.
- Circular JSON serialization between related entities.
- Making API responses dependent on database structure.

DTOs help prevent these problems.

### What does `@Data` do?

Lombok generates:

- Getters.
- Setters.
- `toString()`.
- `equals()`.
- `hashCode()`.
- Required-arguments constructor for applicable fields.

You don't need to write all those methods manually.

# 8. Important code review — changes I'd make

Your architecture is good, but I found some issues worth fixing.



Important

&#x20;1\. Your GET and PUT endpoints sometimes accept JWT but don't use it



For example:

```
@GetMapping("/{id}")
public ResponseEntity<StoreDto> getStoreById(
        @PathVariable("id") Long id,
        @RequestHeader("Authorization") String jwt) {

    return ResponseEntity.ok(storeService.getStoreById(id));
}
```

You accept the JWT but never pass it to the service.

The same issue exists in:

- `getAllStores()`
- `updateStore()`
- `deleteStore()`

This isn't automatically a vulnerability: Spring Security may already authenticate and authorize the request through the filter chain.

But the authorization rules must exist somewhere.

For example, your service's `getStoreById()` currently fetches any store by ID without checking whether the caller can access it.

My recommendation: Use `SecurityContextHolder` through `UserService.getCurrentUser()` and apply the appropriate ownership checks in the service. Also configure endpoint authentication in `SecurityConfig`.

You don't need to manually pass JWT headers into every controller method if your JWT filter already handles them.

### 2. Don't trust the frontend for store metadata

Your mapper currently accepts these values:

```
store.setId(storeDto.getId());
store.setCreatedAt(storeDto.getCreatedAt());
store.setUpdatedAt(storeDto.getUpdatedAt());
store.setStoreStatus(storeDto.getStoreStatus());
```

For a new store, these fields should generally be controlled by the backend.

For example, a client could try:

```
{
  "brand": "Fake Store",
  "storeStatus": "ACTIVE",
  "createdAt": "2020-01-01T00:00:00"
}
```

A new store should generally start in a backend-defined status, such as `PENDING`, if that matches your business rules.

I would change the mapper to:

```
public static Store toEntity(StoreDto dto, User user) {

    Store store = new Store();

    store.setBrand(dto.getBrand());
    store.setDescription(dto.getDescription());
    store.setStoreAdmin(user);
    store.setStoreType(dto.getStoreType());
    store.setContact(dto.getContact());

    store.setStoreStatus(StoreStatus.PENDING);
    store.setCreatedAt(LocalDateTime.now());
    store.setUpdatedAt(LocalDateTime.now());

    return store;
}
```

This assumes `PENDING` is an actual value in your enum and is the desired initial status.

For updates, update only fields the current user is permitted to modify.

### 3. Add `@Transactional` to createStore()

This is particularly important.

Your current flow performs two database writes:

```
Store savedStore = storeRepository.save(store);

user.setStore(savedStore);
userService.updateUser(user);
```

Imagine the first save succeeds but the second fails.

You could end up with a Store record that exists, but the User relationship hasn't been updated.

Add:

```
@Transactional
public StoreDto createStore(StoreDto storeDto, User user) {
    // Existing logic
}
```

Now, if a database operation fails and the exception causes rollback, both writes can be rolled back together.

Make sure `@Transactional` is imported from:

```
import org.springframework.transaction.annotation.Transactional;
```

### 4. Your update method has inconsistent null handling

Currently:

```
existingStore.setBrand(storeDto.getBrand());
existingStore.setDescription(storeDto.getDescription());
```

These two fields are overwritten even if the incoming values are null.

But your store type uses:

```
if (storeDto.getStoreType() != null) {
    existingStore.setStoreType(storeDto.getStoreType());
}
```

So the behavior is inconsistent.

Decide whether this endpoint is a full update (`PUT`) or a partial update (`PATCH`).

For a full update, validate all required fields.

For a partial update, update only the non-null fields, with a separate way to intentionally clear a field if necessary.

### 5. Improve StoreDto

I would change:

```
public Long id;
public String brand;
private String Description;
private StoreContact contact;
```

To something like:

```
@Data
public class StoreDto {

    private Long id;

    private String brand;

    private UserDto storeAdmin;

    private CategoryDto category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String description;

    private String storeType;

    private StoreStatus storeStatus;

    private StoreContactDto contact;
}
```

Changes:

- Use `private` fields consistently.
- Rename `Description` to `description` following Java naming conventions.
- Prefer a `StoreContactDto` instead of exposing the persistence model `StoreContact` directly.

You haven't shared your Store entity, so I can't yet confirm whether `StoreContact` is an embeddable object, a separate entity, or something else.

### 6. Add validation to incoming DTOs

Your controller currently uses:

```
@RequestBody StoreDto storeDto
```

Consider:

```
@Valid @RequestBody StoreDto storeDto
```

And in your DTO:

```
@NotBlank(message = "Brand is required")
private String brand;
```

This lets Spring validate incoming data before calling your service.

### 7. StoreContact and Category fields

You have:

```
private CategoryDto category;
private StoreContact contact;
```

A couple of observations:

- Your mapper doesn't currently map `category`, so the category will not be populated by `toDTO()` or `toEntity()`.
- `contact` is copied directly from the DTO into the entity and back. This may be okay depending on your entity mapping, but a dedicated contact DTO would give you better control over API data.

### 8. Improve the HTTP response for store creation

Your current controller returns:

```
return ResponseEntity.ok(
        storeService.createStore(storeDto, user)
);
```

This returns HTTP 200.

For a successful resource creation, HTTP 201 Created is more appropriate:

```
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(storeService.createStore(storeDto, user));
```

# 9. Overall architecture — how everything connects

## Store management architecture

StoreController

HTTP requests and responses

StoreService

Defines store operations

StoreServiceImp

Business logic and authorization

StoreRepository

Database access

UserService / EmployeeService

Related operations

Store Entity

Database representation

StoreMapper → StoreDto

Converts entity into API response

## 10. Interview questions

| Question                     | Answer                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| Why use a Service interface? | To define a contract and separate business logic from the controller.                  |
| What is StoreMapper?         | A class responsible for converting DTOs into entities and vice versa.                  |
| What is a derived query?     | Spring Data JPA generates a query based on the repository method name.                 |
| What is `@Cacheable`?        | It stores method results in a cache to avoid repeated computation or database queries. |
| What is `@CacheEvict`?       | It removes cached data when it becomes outdated.                                       |
| Why use `@Transactional`?    | To execute related database operations as one transaction.                             |
| Why use DTOs?                | To control the data exposed through API requests and responses.                        |
| Why use `ResponseEntity`?    | To control the HTTP status, headers and response body.                                 |
| Why check store ownership?   | To prevent one store admin from modifying another store's data.                        |

## My final review

Good architecture

You have a sensible structure. I wouldn't rewrite the entire store module.

These are the changes I'd prioritize:

| Priority | Change                                                                                                  |
| -------- | ------------------------------------------------------------------------------------------------------- |
| Critical | Enforce authorization on `getStoreById()` and `getAllStores()` according to the intended access policy. |
| Critical | Don't accept store ID, status, or timestamps from the client during creation.                           |
| High     | Add `@Transactional` to store creation.                                                                 |
| High     | Explicitly restrict `getStoresForAdmin()` to authorized roles.                                          |
| High     | Ensure branch employees can resolve their store where appropriate.                                      |
| Medium   | Improve DTO validation and contact mapping.                                                             |
| Medium   | Return HTTP 201 when creating a store.                                                                  |

One particularly important point: your `StoreController` accepts JWT headers, but some methods don't use those headers. You should have one consistent authentication approach: let your JWT filter authenticate the request, then use `SecurityContextHolder` and your authorization service to identify and authorize the current user.

## What I'd like to inspect next

To verify this module completely, the most useful missing classes are:

1. `Store.java` — to understand the database relationships.
2. `StoreContact.java` — to understand how contact information is stored.
3. `StoreStatus.java` — to understand store moderation.
4. `SecurityConfig.java` — to verify endpoint protection.

My suggestion: Send `Store.java` and `StoreContact.java` next. Then we can understand the actual database relationship between User and Store, including the foreign keys and how JPA handles them.