package com.molla.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Run this as a standalone Java application to generate password hashes
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        // Generate hash for Kaif0786@
        String password = "Kaif0786@";
        String hash = encoder.encode(password);
        
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("========================================");
        System.out.println("\nCopy the hash above and use it in the SQL UPDATE script:");
        System.out.println("UPDATE user SET password = '" + hash + "' WHERE email = 'kaiftruth101@gmail.com';");
        System.out.println("========================================");
    }
}

