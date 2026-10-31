package com.molla.payload.dto;

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
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    private Double amount;
}
