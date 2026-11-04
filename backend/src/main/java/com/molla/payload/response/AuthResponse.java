package com.molla.payload.response;

import lombok.Data;
import com.molla.payload.dto.UserDto;

@Data
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;

}
