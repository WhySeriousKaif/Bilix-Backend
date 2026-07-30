package com.molla.payload.dto;

import java.time.LocalDateTime;

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
    private Integer quantity;
    private Double price;
    private ProductDto product;
    private Long orderId;
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
