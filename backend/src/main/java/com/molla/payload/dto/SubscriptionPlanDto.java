package com.molla.payload.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionPlanDto {
    private Long id;
    private String name;
    private Double price;
    private Integer maxBranches;
    private Integer maxUsers;
    private Integer maxProducts;
    private String features;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
