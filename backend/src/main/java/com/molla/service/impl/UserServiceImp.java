package com.molla.service.impl;

import com.molla.exceptions.NotFoundException;
import com.molla.model.User;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import com.molla.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public User getUserFromJwt(String jwt) {
        // Clean JWT token: Remove "Bearer " prefix if present and trim whitespace
        String cleanToken = jwt != null ? jwt.trim() : "";
        if (cleanToken.startsWith("Bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }

        if (cleanToken.isEmpty()) {
            throw new NotFoundException("JWT token is required");
        }

        String email = jwtUtil.extractEmail(cleanToken);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

    @Override
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found: " + email);
        }
        return user;
    }

    @Override
    @Cacheable(cacheNames = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @Override
    @Cacheable(cacheNames = "usersAll")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
