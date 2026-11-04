package com.molla.service;

import com.molla.payload.dto.LoginRequest;
import com.molla.payload.dto.RegisterRequest;
import com.molla.payload.response.AuthResponse;

public interface AuthService {


    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
