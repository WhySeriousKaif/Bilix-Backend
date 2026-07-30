package com.molla.payload.response;

import org.springframework.stereotype.Component;
import lombok.Data;
import com.molla.payload.dto.UserDto;

@Data
@Component
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;

}
