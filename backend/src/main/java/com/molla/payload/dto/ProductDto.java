package com.molla.payload.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProductDto {

    public Long id;

    public String name;

    public String description;

    public String sku;

    public Double mrp;
    public Double sellingPrice;

    public String brand;
    public String image;
    
    public Integer quantity; // Product stock quantity

    private Long storeId;
    private Long categoryId;
    private CategoryDto category;

    // public StoreDto store; 

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

