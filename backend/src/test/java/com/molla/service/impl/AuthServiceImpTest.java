package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.model.User;
import com.molla.payload.dto.LoginRequest;
import com.molla.payload.dto.RegisterRequest;
import com.molla.payload.response.AuthResponse;
import com.molla.repository.UserRepository;
import com.molla.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImp authService;

    @Test
    void registerHashesPasswordAndReturnsTokenForStoreAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Store Owner")
                .email("owner@example.com")
                .phone("9999999999")
                .password("plain-password")
                .role(UserRole.ROLE_STORE_ADMIN)
                .build();

        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtUtil.generateToken("owner@example.com", "ROLE_STORE_ADMIN"))
                .thenReturn("signed-jwt");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("hashed-password", savedUser.getValue().getPassword());
        assertNotNull(savedUser.getValue().getCreatedAt());
        assertEquals("signed-jwt", response.getJwt());
        assertEquals("User registered successfully", response.getMessage());
        assertEquals("owner@example.com", response.getUser().getEmail());
    }

    @Test
    void registerRejectsAnExistingEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("owner@example.com")
                .password("password")
                .role(UserRole.ROLE_STORE_ADMIN)
                .build();
        when(userRepository.findByEmail("owner@example.com")).thenReturn(new User());

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> authService.register(request));

        assertEquals("User already exists with email: owner@example.com", error.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsEmployeeRolesFromPublicSignup() {
        RegisterRequest request = RegisterRequest.builder()
                .email("cashier@example.com")
                .password("password")
                .role(UserRole.ROLE_CASHIER)
                .build();

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> authService.register(request));

        assertEquals("Cannot self-register with role: ROLE_CASHIER", error.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginDelegatesCredentialCheckingToAuthenticationManager() {
        LoginRequest request = LoginRequest.builder()
                .email("owner@example.com")
                .password("plain-password")
                .build();
        User user = user("owner@example.com", UserRole.ROLE_STORE_ADMIN);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtUtil.generateToken("owner@example.com", "ROLE_STORE_ADMIN"))
                .thenReturn("signed-jwt");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertNotNull(user.getLastLoginAt());
        assertEquals("signed-jwt", response.getJwt());
        assertEquals("Login successfully", response.getMessage());
    }

    @Test
    void loginReturnsOneSafeErrorForInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("owner@example.com")
                .password("wrong-password")
                .build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("internal authentication message"));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", error.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    private User user(String email, UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setFullName("Store Owner");
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(role);
        return user;
    }
}
