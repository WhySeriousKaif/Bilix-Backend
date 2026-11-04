package com.molla.payload.dto;

import java.time.LocalDateTime;

/** Customer activity derived only from orders placed at the requested branch. */
public record BranchCustomerDto(
        Long id,
        String name,
        String email,
        String phone,
        Long orderCount,
        Double totalSpent,
        LocalDateTime lastOrderAt) {
}
