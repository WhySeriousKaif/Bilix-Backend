package com.molla.mapper;

import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public class UserMapper {

    /**
     * 📦 Convert User Entity to User DTO
     * "UserMapper converts between User entity and UserDto to separate database models from API responses."
     * 
     * 👉 Purpose: Map User entity to User DTO for API response
     * 🔹 Flow: Map fields → Set branch/store IDs if available
     * 
     * 📌 UserDto Structure:
     * - id: User ID
     * - fullName: User's full name
     * - email: User's email
     * - phone: User's phone number
     * - role: User's role
     * - branchId: Branch ID (if available)
     * - storeId: Store ID (if available)
     * - createdAt: User creation timestamp
     * - updatedAt: Last update timestamp
     * - lastLoginAt: Last login timestamp
     */
    public static UserDto toDto(User newUser) {
        UserDto userDto = new UserDto();
        userDto.setId(newUser.getId());
        userDto.setFullName(newUser.getFullName());
        userDto.setEmail(newUser.getEmail());
        userDto.setPhone(newUser.getPhone());
        userDto.setRole(newUser.getRole());
        
        // Set branchId and storeId if they exist
        if (newUser.getBranch() != null) {
            userDto.setBranchId(newUser.getBranch().getId());
        }
        if (newUser.getStore() != null) {
            userDto.setStoreId(newUser.getStore().getId());

        }
        
        userDto.setCreatedAt(newUser.getCreatedAt());
        userDto.setUpdatedAt(newUser.getUpdatedAt());
        userDto.setLastLoginAt(newUser.getLastLoginAt());
        userDto.setBranchId(newUser.getBranch()!=null?newUser.getBranch().getId():null);
        userDto.setStoreId(newUser.getStore()!=null?newUser.getStore().getId():null );
        return userDto;
    }

    public static User toEntity(UserDto userDto, PasswordEncoder encoder) {
        User createdUser = new User();
        createdUser.setFullName(userDto.getFullName());
        if (userDto.getPassword() != null && encoder != null) {
            createdUser.setPassword(encoder.encode(userDto.getPassword()));
        }
        createdUser.setEmail(userDto.getEmail());
        createdUser.setPhone(userDto.getPhone());
        createdUser.setRole(userDto.getRole());
        createdUser.setCreatedAt(LocalDateTime.now());
        createdUser.setUpdatedAt(LocalDateTime.now());
        createdUser.setLastLoginAt(LocalDateTime.now());
        return createdUser;
    }
}
