package com.molla.exceptions;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 🛡️ Global Exception Handler - Centralized error handling for all controllers
 * 
 * 👉 Purpose: Catch all exceptions and return consistent JSON error responses
 * 🔹 Why: Provides uniform error format across entire API (better frontend
 * integration)
 * 
 * 📌 How it works:
 * - @RestControllerAdvice: Intercepts exceptions from all @RestController
 * classes
 * - @ExceptionHandler: Maps specific exceptions to handler methods
 * - Returns: Consistent JSON with timestamp, error type, message, and HTTP
 * status
 */
@Hidden // 🔇 Hide from Swagger UI (not an API endpoint)
@Slf4j
@RestControllerAdvice(basePackages = "com.molla.controllers")
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return build(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return build(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage());
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException ex) {
        return build(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid or missing required field");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred");
    }

    /**
     * 🏗️ Build Error Response - Helper method for consistent error format
     * 
     * 👉 Purpose: Create uniform JSON error response structure
     * 🔹 Response format:
     * {
     * "timestamp": "2026-01-14T19:49:20.729Z",
     * "error": "NOT_FOUND",
     * "message": "User not found with id: 123",
     * "status": 404
     * }
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString()); // ⏰ When error occurred
        body.put("error", error); // 🔖 Error type (NOT_FOUND, BAD_REQUEST, etc.)
        body.put("message", message); // 📝 Human-readable error message
        body.put("status", status.value()); // 🔢 HTTP status code
        return ResponseEntity.status(status).body(body);
    }
}
