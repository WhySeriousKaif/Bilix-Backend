package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.UserMapper;
import com.molla.model.User;
import com.molla.payload.dto.LoginRequest;
import com.molla.payload.dto.RegisterRequest;
import com.molla.payload.response.AuthResponse;
import com.molla.repository.UserRepository;
import com.molla.service.AuthService;
import com.molla.service.MailService;
import com.molla.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImp implements AuthService {

    // Public signup represents a business owner creating a store-admin account.
    // Employee roles are created later by an authorized manager.
    private static final Set<UserRole> SELF_REGISTERABLE_ROLES = Set.of(
            UserRole.ROLE_STORE_ADMIN);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Optional because email is not configured in every environment.
    @Autowired(required = false)
    private MailService mailService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = { "users", "usersAll" }, allEntries = true)
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new BadRequestException("User already exists with email: " + request.getEmail());
        }

        if (!SELF_REGISTERABLE_ROLES.contains(request.getRole())) {
            throw new BadRequestException("Cannot self-register with role: " + request.getRole());
        }

        LocalDateTime now = LocalDateTime.now();

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(request.getRole());
        newUser.setFullName(request.getFullName());
        newUser.setPhone(request.getPhone());
        newUser.setLastLoginAt(now);
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        User savedUser = userRepository.save(newUser);

        sendWelcomeEmail(savedUser);

        String jwt = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return buildResponse(savedUser, jwt, "User registered successfully");
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException exception) {
            throw new BadRequestException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            // Authentication normally guarantees that the user exists.
            throw new NotFoundException("User not found: " + request.getEmail());
        }

        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        String jwt = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return buildResponse(savedUser, jwt, "Login successfully");
    }

    private void sendWelcomeEmail(User user) {
        if (mailService == null) {
            return;
        }

        try {
            mailService.sendSimpleMail(
                    user.getEmail(),
                    "Welcome to Bilix POS",
                    "Hi " + user.getFullName() + ",\n\n"
                            + "Your account has been created successfully.\n\n"
                            + "Role: " + user.getRole() + "\n\n"
                            + "Regards,\nBilix POS System");
        } catch (Exception exception) {
            // Email is best-effort; account creation remains successful.
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), exception.getMessage());
        }
    }

    private AuthResponse buildResponse(User user, String jwt, String message) {
        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setMessage(message);
        response.setUser(UserMapper.toDto(user));
        return response;
    }
}
