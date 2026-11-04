package com.molla.controllers;

import com.molla.mapper.UserMapper;
import com.molla.model.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;

import com.molla.service.UserService;
import com.molla.payload.dto.UserDto;   
import java.util.List;
import org.springframework.data.domain.Page;
import com.molla.domain.UserRole;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getUserProfile() {
        return ResponseEntity.ok(UserMapper.toDto(userService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(UserMapper.toDto(userService.getUserById(id)));

    }
    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(UserMapper::toDto).toList());
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
