# Store Creation and Entity–DTO Mapping Flow

## 1. Complete flow

```text
Frontend sends StoreDto + JWT
        ↓
StoreController receives request
        ↓
JWT identifies the logged-in user
        ↓
UserService loads the complete User entity
        ↓
StoreService checks role and existing store
        ↓
StoreMapper.toEntity(storeDto, user)
        ↓
StoreRepository saves Store entity
        ↓
User is linked to the saved Store
        ↓
StoreMapper.toDTO(savedStore)
        ↓
StoreDto JSON response
```

## 2. Request from the frontend

```json
{
  "brand": "Molla Store",
  "description": "Main store",
  "storeType": "RETAIL",
  "contact": {
    "address": "Main Street",
    "phone": "9876543210",
    "email": "store@example.com"
  }
}
```

The request also contains:

```http
Authorization: Bearer <jwt>
```

The controller receives both values:

```java
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

```text
@RequestBody StoreDto
    = converts JSON body into StoreDto

@RequestHeader("Authorization")
    = receives the JWT header
```

## 3. Why is the user obtained separately?

The frontend sends store information, but it should not decide who owns the store. The backend gets the owner from the authenticated JWT:

```java
User user = userService.getUserFromJwt(jwt);
```

The service flow is:

```text
JWT
  ↓
Extract email
  ↓
UserRepository.findByEmail(email)
  ↓
Return complete User entity
```

This prevents a user from sending another person's ID and pretending to create a store for them.

## 4. Service validation

The controller passes both the DTO and authenticated user to the service:

```java
storeService.createStore(storeDto, user);
```

The service first checks the role:

```java
if (!user.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
    throw new BadRequestException(
        "Only a Store Admin can create a store"
    );
}
```

It then prevents the same user from creating multiple stores:

```java
Store existingStore =
    storeRepository.findByStoreAdminId(user.getId());

if (existingStore != null) {
    throw new BadRequestException(
        "User already has a store"
    );
}
```

## 5. `StoreMapper.toEntity()`

The mapper converts the API DTO into a database entity:

```java
Store store = StoreMapper.toEntity(storeDto, user);
```

The project mapper performs this conversion:

```java
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

The important line is:

```java
store.setStoreAdmin(user);
```

The authenticated user becomes the store admin. The backend does not trust a user identity sent in the request body.

## 6. `StoreContact` and `@Embeddable`

The store contains:

```java
private StoreContact contact = new StoreContact();
```

`StoreContact` is an embeddable value object:

```java
@Embeddable
public class StoreContact {
    private String address;
    private String phone;
    private String email;
}
```

Its fields are stored in the store table instead of a separate contact table:

```text
stores
----------------------------------------------------------
id | brand | address | phone | email | store_admin_id
```

## 7. Saving the entity

The service saves the entity:

```java
Store savedStore = storeRepository.save(store);
```

The repository is:

```java
public interface StoreRepository
        extends JpaRepository<Store, Long> {

    Store findByStoreAdminId(Long adminId);
}
```

JPA/Hibernate generates the SQL `INSERT` and returns the saved entity with its generated ID.

## 8. Linking the user and store

After the store is saved:

```java
user.setStore(savedStore);
userService.updateUser(user);
```

The Java relationships are:

```java
// Store.java
@OneToOne
private User storeAdmin;
```

```java
// User.java
@ManyToOne
private Store store;
```

Business meaning:

```text
One store has one store admin.
Many users can belong to one store.
```

The service updates both sides so the application can navigate from store to admin and from user to store.

## 9. `StoreMapper.toDTO()`

After saving, the entity is converted into a safe response DTO:

```java
return StoreMapper.toDTO(savedStore);
```

```java
public static StoreDto toDTO(Store store) {
    StoreDto dto = new StoreDto();

    dto.setId(store.getId());
    dto.setBrand(store.getBrand());
    dto.setDescription(store.getDescription());
    dto.setStoreType(store.getStoreType());
    dto.setContact(store.getContact());
    dto.setStoreStatus(store.getStoreStatus());

    if (store.getStoreAdmin() != null) {
        dto.setStoreAdmin(
            UserMapper.toDto(store.getStoreAdmin())
        );
    }

    return dto;
}
```

The response may look like:

```json
{
  "id": 1,
  "brand": "Molla Store",
  "description": "Main store",
  "storeType": "RETAIL",
  "storeStatus": "PENDING",
  "storeAdmin": {
    "id": 5,
    "fullName": "Kaif",
    "email": "kaif@example.com"
  }
}
```

## 10. DTO, entity and mapper responsibilities

```text
StoreDto
    = request/response API object

Store
    = database entity

User
    = authenticated owner loaded from JWT/database

StoreMapper
    = converts StoreDto ↔ Store

StoreRepository
    = saves and queries Store entities
```

## 11. Interview answer

> When a store is created, the frontend sends store data as a `StoreDto` and sends a JWT in the Authorization header. The controller uses the JWT to load the authenticated `User`. The service verifies that the user is a Store Admin and does not already own a store. `StoreMapper.toEntity()` copies the DTO fields into a `Store` entity and attaches the authenticated user as `storeAdmin`. The repository saves the entity through JPA. The user is then linked back to the saved store. Finally, `StoreMapper.toDTO()` converts the saved entity into a safe response object for the frontend.

## 12. Final memory shortcut

```text
JWT user      = who is creating the store
StoreDto      = store data from/to the API
StoreMapper   = DTO ↔ entity conversion
Store          = database object
Repository    = database operation
UserMapper    = safe nested admin response
```

## 13. Understanding `dtos.stream()` in `getAllStores()`

The repository returns a list of database entities:

```java
List<Store> dtos = storeRepository.findAll();
```

The variable name `dtos` is slightly confusing here. At this point the list contains `Store` entities, not DTOs. A clearer name would be:

```java
List<Store> stores = storeRepository.findAll();
```

The project then converts every `Store` entity into a `StoreDto`:

```java
return stores.stream()
    .map(StoreMapper::toDTO)
    .collect(Collectors.toList());
```

### Step-by-step meaning

```text
stores
    = [Store, Store, Store]

stream()
    = starts a pipeline for processing each Store

map(StoreMapper::toDTO)
    = converts every Store into a StoreDto

collect(Collectors.toList())
    = gathers all converted values into a List

return
    = sends List<StoreDto> back to the controller
```

The method reference:

```java
StoreMapper::toDTO
```

is a shorter form of:

```java
store -> StoreMapper.toDTO(store)
```

So this:

```java
return stores.stream()
    .map(StoreMapper::toDTO)
    .collect(Collectors.toList());
```

means the same as this traditional loop:

```java
List<StoreDto> result = new ArrayList<>();

for (Store store : stores) {
    result.add(StoreMapper.toDTO(store));
}

return result;
```

### Why do we map the list?

The database returns entities, but the API should return DTOs:

```text
List<Store>
    ↓ map each item
List<StoreDto>
```

This prevents internal entity fields from being exposed and keeps the response format controlled.

### Important detail

The mapped result must be returned. This line alone does not return anything:

```java
stores.stream()
    .map(StoreMapper::toDTO)
    .collect(Collectors.toList());
```

The correct method needs `return`:

```java
return stores.stream()
    .map(StoreMapper::toDTO)
    .collect(Collectors.toList());
```

### MERN comparison

```javascript
const stores = await Store.find();

const storeDtos = stores.map(store => ({
  id: store._id,
  brand: store.brand,
  status: store.storeStatus
}));

res.json(storeDtos);
```

Java `stream().map(...)` is conceptually similar to JavaScript `.map(...)`.
