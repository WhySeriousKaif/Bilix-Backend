package com.molla.service.impl;

import com.molla.exceptions.NotFoundException;
import com.molla.model.User;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import com.molla.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.molla.domain.UserRole;
import java.util.Set;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AccessControlService accessControlService;

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
        User target = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        accessControlService.requireUserAccess(getCurrentUser(), target);
        return target;
    }


    @Override
    @Cacheable(cacheNames = "usersAll")
    public List<User> getAllUsers() {
        accessControlService.requireAdmin(getCurrentUser());
        return userRepository.findAll();
    }

    @Override
    public Page<User> getUsersPaged(int page, int size, String sortBy,
                                    String direction, String search, UserRole role) {
        accessControlService.requireAdmin(getCurrentUser());
        page = Math.max(page, 0);
        size = size <= 0 || size > 100 ? 10 : size;

        Set<String> allowedSorts = Set.of("id", "fullName", "email", "role", "createdAt");

        sortBy = allowedSorts.contains(sortBy) ? sortBy : "createdAt";

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        return userRepository.findAllFiltered(normalizedSearch, role, PageRequest.of(page, size, sort));
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = { "users", "usersAll" },
            allEntries = true
    )
    public User updateUser(User user) {

        return userRepository.save(user);
    }
}
