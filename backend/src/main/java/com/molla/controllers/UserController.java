package com.molla.controllers;

import com.molla.mapper.UserMapper;
import com.molla.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.molla.service.UserService;
import com.molla.payload.dto.UserDto;   
import com.molla.exceptions.UserException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import com.molla.domain.UserRole;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/profile")

    public ResponseEntity<UserDto> getUserProfile(@RequestHeader(value = "Authorization", required = false) String jwt) throws UserException {
        // For testing, if no JWT, return a default user with branch
        if (jwt == null || jwt.isEmpty()) {
            UserDto defaultUser = new UserDto();
            defaultUser.setId(1L);
            defaultUser.setFullName("Test Cashier");
            defaultUser.setEmail("cashier@test.com");
            defaultUser.setBranchId(1L);
            return ResponseEntity.ok(defaultUser);
        }
        User user = userService.getUserFromJwt(jwt);
        return  ResponseEntity.ok(UserMapper.toDto(user));

    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id ) throws UserException {
        User user = userService.getUserById(id);
        if(user==null){
            throw new UserException("User not found");
        }
        return  ResponseEntity.ok(UserMapper.toDto(user));

    }
    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() throws UserException {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(UserMapper::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<UserDto>> getUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role) {
        return ResponseEntity.ok(userService
                .getUsersPaged(page, size, sortBy, direction, search, role)
                .map(UserMapper::toDto));
    }
}
