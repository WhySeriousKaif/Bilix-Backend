package com.molla.payload.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundItemDto {
    private Long id;

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    private Long productId;
    private String productName;

    @NotNull(message = "Refund quantity is required")
    @Positive(message = "Refund quantity must be greater than zero")
    private Integer quantity;
    private Double unitPrice;
    private Double amount;
}
