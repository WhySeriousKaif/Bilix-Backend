package com.molla.service.impl;

import com.molla.model.User;
import com.molla.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * 👤 Custom User Details Service - Spring Security integration
 * 
 * 👉 Purpose: Load user from database and convert to Spring Security UserDetails
 * 🔹 Why: Spring Security needs UserDetails to authenticate and authorize users
 * 
 * 📌 Flow:
 * 1. Receive email (username) from Spring Security
 * 2. Load User entity from database
 * 3. Convert UserRole to Spring Security GrantedAuthority
 * 4. Return UserDetails object (email, password, authorities)
 * 
 * 🔗 Used by: JwtFilter (loads user during JWT validation)
 */
@Service
public class CustomUserImplementation implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 📥 Load User by Username (email)
     * 
     * 👉 Purpose: Spring Security calls this to load user during authentication
     * 🔹 Input: Email (username parameter)
     * 🔹 Output: UserDetails with email, password, and authorities (roles)
     * 
     * 📌 Note: "username" parameter is actually email in our system
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 📥 Step 1: Fetch user from database using email
        User user = userRepository.findByEmail(username);

        // ❌ Step 2: If user not found → throw exception
        // 🔹 Spring Security catches this and rejects authentication
        if (user == null) {
            throw new UsernameNotFoundException(
                    "User not found with email: " + username
            );
        }

        // 🔐 Step 3: Convert UserRole to Spring Security GrantedAuthority
        // 🔹 Example: ROLE_STORE_ADMIN → GrantedAuthority("ROLE_STORE_ADMIN")
        GrantedAuthority authority =
                new SimpleGrantedAuthority(user.getRole().toString());

        Collection<GrantedAuthority> authorities =
                Collections.singletonList(authority);

        // ✅ Step 4: Return Spring Security UserDetails object
        // 🔹 Contains: email (username), password (hashed), authorities (roles)
        // 🔹 Spring Security uses this for authentication and authorization
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),  // Username (email in our case)
                user.getPassword(),  // Hashed password (for comparison)
                authorities  // Roles/permissions (for authorization)
        );
    }
}
