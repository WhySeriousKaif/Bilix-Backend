package com.molla.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔑 JWT Utility - Handles JWT token generation and validation
 * 
 * 👉 Purpose: Centralized JWT operations (generate, extract, validate)
 * 🔹 Why: Reusable utility component following single responsibility principle
 * 
 * 📌 JWT Structure:
 * - Header: Algorithm (HS256) and token type
 * - Payload: Claims (email, role, issuedAt, expiration)
 * - Signature: HMAC-SHA256 hash of header + payload + secret key
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    // ⏰ Token expiration: 24 hours (in milliseconds)
    private static final long EXPIRATION = 1000 * 60 * 60 * 24;

    /**
     * 🔐 Constructor - Initialize secret key from properties
     * 
     * 👉 Purpose: Load JWT secret from application.properties or environment variable
     * 🔹 Default: Uses hardcoded secret for local development (NOT for production!)
     */
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // 🔑 Create HMAC-SHA256 secret key from string
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 🎫 Generate JWT Token
     * 
     * 👉 Purpose: Create signed JWT token with user email and role
     * 🔹 Flow: Build claims → Set subject (email) → Set expiration → Sign with secret → Return token string
     * 
     * 📌 Token Contents:
     * - Subject: User's email (used to identify user)
     * - Role: User's role (for authorization)
     * - Issued At: When token was created
     * - Expiration: 24 hours from creation
     * 
     * {
  "sub": "kaif@gmail.com",
  "role": "ADMIN",
  "iat": 1700000000,
  "exp": 1700086400
  Java Map
   ↓
JWT Payload (JSON)
   ↓
Base64 Encode
   ↓
Signed JWT String
   ↓
Parse + Verify
   ↓
Claims Map
   ↓
getSubject() → email
}
     */
    public String generateToken(String email, String role) {
        // 📝 Create claims map (custom data in token)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        // 🔨 Build JWT token using jjwt library
        return Jwts.builder()
                .setClaims(claims)  // Add custom claims (role)
                .setSubject(email)  // Set subject (email - identifies user)
                .setIssuedAt(new Date())  // Token creation time
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))  // 24 hours expiry
                .signWith(key)  // Sign with secret key (prevents tampering)
                .compact();  // Convert to compact string format // like base64 hash function
    }


    /**
     * 📧 Extract Email from Token
     * 
     * 👉 Purpose: Get user's email from JWT token (subject claim)
     * 🔹 Why: Email is stored as "subject" in JWT standard
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 👤 Extract Role from Token
     * 
     * 👉 Purpose: Get user's role from JWT token (custom claim)
     * 🔹 Why: Role is needed for authorization checks
     * {
  "sub": "kaif@gmail.com",
  "role": "ADMIN",
  "iat": 1700000000,
  "exp": 1700086400
  Claims claims = extractAllClaims(token);

  
  Claims (DefaultClaims)
│
├── "sub"  → "kaif@gmail.com"     (String)
├── "role" → "ADMIN"             (String)
├── "iat"  → Date(1700000000)    (Date)
└── "exp"  → Date(1700086400)    (Date)
}
     */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /**
     * ✅ Validate Token
     * 
     * 👉 Purpose: Check if token is valid and not expired
     * 🔹 Validation checks:
     * 1. Token email matches provided email
     * 2. Token is not expired
     * 
     * 📌 Returns: true if valid, false if invalid/expired
     */
    public boolean validateToken(String token, String email) {
        try {
            return extractEmail(token).equals(email) && !isExpired(token);
        } catch (Exception e) {
            // ⚠️ Any parsing error = invalid token
            return false;
        }
    }

    /**
     * ⏰ Check if Token is Expired
     * 
     * 👉 Purpose: Verify token hasn't passed expiration time
     * 🔹 Why: Expired tokens should be rejected (security)
     */
    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * 🔍 Extract All Claims from Token
     * 
     * 👉 Purpose: Parse JWT token and extract all claims (payload)
     * 🔹 Flow: Parse token → Verify signature → Extract claims
     * 
     * 📌 Security: Signature verification ensures token wasn't tampered with
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)  // Use same secret key used for signing
                .build()
                .parseClaimsJws(token)  // Parse and verify signature
                .getBody();  // Extract claims (payload)
    }
}
