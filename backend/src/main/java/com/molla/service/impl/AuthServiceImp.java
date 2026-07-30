package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.UserException;
import com.molla.mapper.UserMapper;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.AuthResponse;
import com.molla.repository.UserRepository;
import com.molla.service.AuthService;
import com.molla.service.MailService;
import com.molla.service.impl.CustomUserImplementation;
import com.molla.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService {

    // Roles a user may assign to themselves via public self-registration.
    // Anything else (admin, store admin, store manager, etc.) must be granted by an existing admin.
    private static final Set<UserRole> SELF_REGISTERABLE_ROLES = Set.of(
            UserRole.ROLE_CASHIER,
            UserRole.ROLE_BRANCH_CASHIER,
            UserRole.ROLE_BRANCH_MANAGER
    );

    private final UserRepository userRepository;

    private  final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private  final CustomUserImplementation customUserImplementation;

    // Optional email service (only active when spring.mail.host is configured)
    @Autowired(required = false)
    private MailService mailService;




    @Override
    public AuthResponse signUp(UserDto user) throws UserException {
        User existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser != null) {
            throw new UserException("User already exists");
        }
        if (!SELF_REGISTERABLE_ROLES.contains(user.getRole())) {
            throw new UserException("Cannot self-register with role: " + user.getRole());
        }

        User newUser=new User();
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setRole(user.getRole());
        newUser.setFullName(user.getFullName());
        newUser.setPhone(user.getPhone());
        newUser.setLastLoginAt(LocalDateTime.now());

        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(newUser);

        // Send welcome email (best-effort – failures are ignored so signup still works)
        if (mailService != null) {
            try {
                mailService.sendSimpleMail(
                        newUser.getEmail(),
                        "Welcome to Molla POS",
                        "Hi " + newUser.getFullName() + ",\n\n" +
                                "Your account has been created successfully.\n\n" +
                                "Role: " + newUser.getRole() + "\n\n" +
                                "Regards,\nMolla POS System"
                );
            } catch (Exception e) {
                // Log and continue (you can hook into a logger if needed)
                System.out.println("Failed to send welcome email: " + e.getMessage());
            }
        }

        Authentication authentication=
                new UsernamePasswordAuthenticationToken(user.getEmail(),user.getPassword());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        String jwt=jwtUtil.generateToken(newUser.getEmail(), newUser.getRole().name());

        AuthResponse authResponse=new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("User registered successfully");
        authResponse.setUser(UserMapper.toDto(newUser));





        return authResponse;
    }

    @Override
    public AuthResponse login(UserDto user) throws UserException {
        String email=user.getEmail();
        String password=user.getPassword();
        Authentication authentication=authenticate(email,password);
        
        // Check if authentication failed
        if (authentication == null) {
            throw new UserException("Invalid email or password");
        }
        
        User foundUser=userRepository.findByEmail(email);
        
        if (foundUser == null) {
            throw new UserException("User not found");
        }
        
        String jwt=jwtUtil.generateToken(foundUser.getEmail(), foundUser.getRole().name());

        foundUser.setLastLoginAt(LocalDateTime.now());

        userRepository.save(foundUser);

        AuthResponse authResponse=new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Login successfully");
        authResponse.setUser(UserMapper.toDto(foundUser));

        return authResponse;
    }

    private  Authentication authenticate(String email,String password){
        try {
        UserDetails userDetails=customUserImplementation.loadUserByUsername(email);
        if(!userDetails.getUsername().equals(email)){
            return  null;
        }

        if(passwordEncoder.matches(password,userDetails.getPassword())){
            return  new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );
        }
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // User not found - return null to indicate authentication failed
            return null;
        }

        return  null;
    }
}
