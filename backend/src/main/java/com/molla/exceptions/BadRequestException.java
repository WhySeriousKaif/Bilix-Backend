package com.molla.exceptions;

/**
 * ⚠️ Bad Request Exception (400)
 * 
 * 👉 Purpose: Throw when request is invalid (validation errors, business rule violations)
 * 🔹 HTTP Status: 400 Bad Request
 * 
 * 📌 Examples:
 * - "User already exists with email: xyz@example.com"
 * - "Cannot register as ADMIN"
 * - "Product with SKU 'ABC123' already exists"
 * 
 * 🔗 Handled by: GlobalExceptionHandler.handleBadRequest()
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
