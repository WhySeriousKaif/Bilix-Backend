# Entity-to-DTO Mapping — Easy Revision Notes

## 1. Why do we map Entity and DTO?

An **Entity** represents database data. A **DTO** represents data exchanged through the API.

```text
Entity = database model
DTO    = API data model
Mapper = converts between them
```

We should not return the entity directly because it may contain sensitive fields such as a password and because the database structure should not become the public API structure.

## 2. Manual mapping

### Entity

```java
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;
}
```

### DTO

```java
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
}
```

The password is intentionally not included in `UserDto`.

### Manual conversion

```java
public UserDto convertToDto(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setName(user.getName());
    dto.setEmail(user.getEmail());
    return dto;
}
```

This is exactly the style used by the project's `UserMapper`: it copies safe entity fields into `UserDto` and also maps related branch/store IDs.

## 3. ModelMapper shortcut

ModelMapper uses reflection to copy fields with matching names.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final ModelMapper modelMapper;

    public UserDto convertToDto(User user) {
        return modelMapper.map(user, UserDto.class);
    }
}
```

This is quick, but automatic mapping can hide mistakes when field names or nested objects are different. Sensitive fields should be excluded or mapped carefully.

## 4. MapStruct shortcut

MapStruct generates mapping code at compile time.

```java
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(UserDto dto);
}
```

Use it in a service:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserDto getUserDto(User user) {
        return userMapper.toDto(user);
    }
}
```

`componentModel = "spring"` registers the generated mapper as a Spring bean, so it can be injected.

MapStruct is often preferred for larger projects because its code is generated at compile time, is fast at runtime and gives compile-time errors for many mapping problems.

## 5. Project-specific mapping flow

The project currently uses a normal Java `UserMapper` with static methods:

```java
UserDto dto = UserMapper.toDto(user);
```

Its main responsibilities are:

```text
User entity → copy safe fields → UserDto
UserDto     → create entity      → User
```

When creating an entity, the password must be encoded first:

```java
createdUser.setPassword(encoder.encode(userDto.getPassword()));
```

Never store a plain-text password and never return the password in a response DTO.

## 6. MERN comparison

| Feature | MERN | Spring Boot |
|---|---|---|
| Manual mapping | Destructuring or object spread | Getters/setters, constructor or builder |
| Remove password | `const { password, ...dto } = user` | Leave password out of DTO |
| Shortcut | `_.pick(user, ['id', 'name'])` | ModelMapper or MapStruct |
| Database object | Mongoose document | JPA entity |
| API object | Plain JavaScript object | DTO class |

MERN example:

```javascript
const { password, ...userDto } = user.toObject();
res.json(userDto);
```

Spring example:

```java
return UserMapper.toDto(user);
```

## 7. Which approach should we choose?

```text
Manual mapping = clear and easy for small projects
ModelMapper    = quick reflection-based shortcut
MapStruct      = explicit, fast compile-time mapping for larger projects
```

For interview purposes, explain that the current project uses manual mapper classes for clear control over which fields are exposed.

## 8. Interview answer

> Entity-to-DTO mapping separates the database model from the API model. The mapper copies only the fields that the frontend should receive, so sensitive fields such as passwords are not exposed. In this project, `UserMapper.toDto()` converts a `User` entity into a `UserDto`, and `toEntity()` performs the reverse conversion. Mapping can be written manually or generated with ModelMapper or MapStruct. Manual mapping gives the clearest control, while MapStruct generates efficient code at compile time.

## 9. Final memory shortcut

```text
Entity = database structure
DTO    = API structure
Mapper = Entity ↔ DTO converter
toDto  = safe response object
toEntity = object ready for persistence
```

## 10. Controller flow in this project

The profile endpoint follows this flow:

```text
Authorization header
        ↓
UserService.getUserFromJwt(jwt)
        ↓
User entity from the database
        ↓
UserMapper.toDto(user)
        ↓
UserDto returned as JSON
```

The controller uses the mapper before returning the response:

```java
User user = userService.getUserFromJwt(jwt);

return ResponseEntity.ok(UserMapper.toDto(user));
```

The entity may contain internal fields such as the password, while `UserDto` contains only fields that the frontend should see.

## 11. Manual static mapper used by this project

Your project uses a normal Java mapper class with a static method:

```java
public class UserMapper {

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());

        if (user.getBranch() != null) {
            dto.setBranchId(user.getBranch().getId());
        }

        if (user.getStore() != null) {
            dto.setStoreId(user.getStore().getId());
        }

        return dto;
    }
}
```

Because the method is static, the controller calls it through the class name:

```java
UserMapper.toDto(user);
```

This is different from an injected MapStruct mapper, which would be called through an object:

```java
userMapper.toDto(user);
```

## 12. MapStruct alternative

MapStruct generates the implementation during compilation:

```java
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
```

The controller then injects it:

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getUserProfile(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}
```

`componentModel = "spring"` makes the generated mapper a Spring bean. This is why it can be injected through the constructor.

## 13. Manual mapper vs MapStruct

```text
Manual static mapper
    = we write every field assignment
    = call UserMapper.toDto(user)
    = the approach currently used in this project

MapStruct mapper
    = we define an interface
    = MapStruct generates the field assignments
    = inject the mapper and call userMapper.toDto(user)
```

MERN comparison:

```javascript
const { password, ...userDto } = user.toObject();
res.json(userDto);
```

Spring comparison:

```java
return ResponseEntity.ok(UserMapper.toDto(user));
```

In both cases, the goal is the same: return a safe API object instead of exposing the full database object.

## 14. Interview answer

> The controller receives the user entity from the service, then passes it to `UserMapper.toDto()`. The mapper copies only public fields into `UserDto`, leaving out sensitive fields such as the password. In this project the mapper is a manually written static utility class. An alternative is MapStruct, where an interface is declared and the implementation is generated at compile time and injected as a Spring bean.

## 15. Converting a list of entities with `stream()`

The variable name `dtos` can be confusing if it actually contains `Store` entities. A clearer name is `stores`:

```java
public List<StoreDto> getAllStores() {
    List<Store> stores = storeRepository.findAll();

    return stores.stream()
            .map(StoreMapper::toDto)
            .collect(Collectors.toList());
}
```

The pipeline means:

```text
stores.stream()                  -> process stores one by one
.map(StoreMapper::toDto)         -> Store entity becomes StoreDto
.collect(Collectors.toList())    -> collect the DTOs into a List
```

`StoreMapper::toDto` is a method reference, equivalent to:

```java
.map(store -> StoreMapper.toDto(store))
```

The same operation with a traditional loop is:

```java
public List<StoreDto> getAllStores() {
    List<Store> stores = storeRepository.findAll();
    List<StoreDto> storeDtos = new ArrayList<>();

    for (Store store : stores) {
        storeDtos.add(StoreMapper.toDto(store));
    }

    return storeDtos;
}
```

With Java 16+, `.toList()` is a shorter alternative to `collect(Collectors.toList())`:

```java
return storeRepository.findAll()
        .stream()
        .map(StoreMapper::toDto)
        .toList();
```

The result is still a `List<StoreDto>`; the stream only provides a concise way to transform every entity.
