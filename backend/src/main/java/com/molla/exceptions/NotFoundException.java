package com.molla.exceptions;

/**
 * ❌ Not Found Exception (404)
 * 
 * 👉 Purpose: Throw when requested resource doesn't exist
 * 🔹 HTTP Status: 404 Not Found
 * 
 * 📌 Examples:
 * - "User not found with id: 123"
 * - "Store not found with id: 456"
 * - "Product not found with id: 789"
 * 
 * 🔗 Handled by: GlobalExceptionHandler.handleNotFound()
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
