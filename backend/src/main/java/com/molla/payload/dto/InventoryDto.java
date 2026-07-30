package com.molla.payload.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDto {
    private Long id;
    private BranchDto branch;
    private ProductDto product;
    private Integer quantity;
    private LocalDateTime lastUpdated;

    // IDs used when creating/updating inventory
    @JsonProperty("branchId")
    @JsonAlias({"branchid", "branch_id"})
    private Long branchId;
    
    @JsonProperty("productId")
    @JsonAlias({"productid", "product_id"})
    private Long productId;
}
