package com.molla.configuration;

import com.molla.service.impl.CustomUserImplementation;
import com.molla.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 🔐 JWT Filter - Validates JWT tokens on every protected request
 * 
 * 👉 Purpose: Extracts JWT from Authorization header, validates it, and sets Spring Security context
 * 🔹 Why: Spring Security needs authentication context to authorize endpoints based on roles
 * 
 * 📌 Flow:
 * 1. Extract "Bearer <token>" from Authorization header
 * 2. Parse token and extract email using JwtUtil
 * 3. Load user from database using email
 * 4. Validate token matches user
 * 5. Set authentication in SecurityContextHolder (so Spring Security knows who's logged in)
 * 6. Continue filter chain (request proceeds to controller)
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserImplementation userService;

    public JwtFilter(JwtUtil jwtUtil, CustomUserImplementation userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    /**
     * 🔍 Core Filter Logic - Runs on every HTTP request
     * 
     * 👉 Purpose: Validate JWT token and authenticate user
     * 🔹 Key: Graceful error handling - public endpoints work even with invalid tokens
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 📥 Step 1: Extract Authorization header
        final String authHeader = request.getHeader("Authorization");

        String token = null;
        String email = null;

        // 🔍 Step 2: Check if header exists and has "Bearer " prefix
        if (authHeader != null && authHeader.trim().startsWith("Bearer ")) {
            try {
                token = authHeader.substring(7).trim();  // Remove "Bearer " prefix and trim whitespace
                if (!token.isEmpty()) {
                    email = jwtUtil.extractEmail(token);  // Extract email from token
                }
            } catch (Exception e) {
                // ⚠️ Invalid token format - continue without authentication
                // 🔹 Why: Allows public endpoints to work even with malformed tokens
            }
        }

        // ✅ Step 3: Authenticate only if email is valid and context is empty
        // 🔹 Why check context? Avoid re-authenticating if already authenticated
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 📥 Step 4: Load user from database using email
                UserDetails userDetails = userService.loadUserByUsername(email);

                // ✅ Step 5: Validate token matches user and is not expired
                if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                    // 🔐 Step 6: Create Spring Security authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,  // Principal (user)
                                    null,  // Credentials (not needed for JWT)
                                    userDetails.getAuthorities()  // Roles/permissions
                            );

                    // 📝 Add request details (IP, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 🔑 Step 7: Set authentication in Spring Security context
                    // 🔹 This tells Spring Security: "This user is authenticated with these roles"
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // ⚠️ User not found or token validation failed - continue without authentication
                // 🔹 Why: Allows public endpoints to work (graceful degradation)
            }
        }

        // ➡️ Step 8: Continue filter chain (request proceeds to next filter or controller)
        filterChain.doFilter(request, response);
    }
}
