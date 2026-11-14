package com.molla.payload.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long id;

    // Optional (checkout defaults a missing quantity to 1), but if a client sends
    // zero or a negative number the request is rejected before it reaches the service.
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;
    private Double price;
    private ProductDto product;
    private Long orderId;
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
