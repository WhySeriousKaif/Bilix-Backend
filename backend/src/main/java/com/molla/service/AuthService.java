package com.molla.service;

import com.molla.exceptions.UserException;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.AuthResponse;

public interface AuthService {


    AuthResponse signUp(UserDto user) throws UserException;
    AuthResponse login(UserDto user) throws UserException;
}
