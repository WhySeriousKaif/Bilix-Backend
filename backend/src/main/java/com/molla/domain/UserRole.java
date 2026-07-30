package com.molla.domain;

public enum UserRole {

    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CASHIER,
    ROLE_BRANCH_MANAGER,
    ROLE_STORE_MANAGER,
    ROLE_STORE_ADMIN,
    ROLE_STORE_EMPLOYEE,
    ROLE_BRANCH_CASHIER;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }

        // 1. Try direct match (e.g., "ROLE_CASHIER")
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        // 2. Try normalizing: "Cashier" -> "CASHIER" -> "ROLE_CASHIER"
        String normalized = value.toUpperCase().replace(" ", "_");

        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            return UserRole.valueOf("ROLE_" + normalized);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        throw new IllegalArgumentException("Unknown user role: " + value);
    }
}
