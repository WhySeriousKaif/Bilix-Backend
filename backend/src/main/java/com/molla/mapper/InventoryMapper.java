package com.molla.mapper;

import com.molla.model.Branch;
import com.molla.model.Inventory;
import com.molla.model.Product;
import com.molla.payload.dto.BranchDto;
import com.molla.payload.dto.InventoryDto;
import com.molla.payload.dto.ProductDto;

public class InventoryMapper {
    public static InventoryDto toDto(Inventory inventory){
        return InventoryDto.builder()
                .id(inventory.getId())
                .branch(BranchMapper.toDto(inventory.getBranch()))
                .product(ProductMapper.toDto(inventory.getProduct()))
                .quantity(inventory.getQuantity())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }

    public static Inventory toEntity(InventoryDto inventoryDto, Branch branch, Product product){
        return Inventory.builder()
                .branch(branch)
                .product(product)
                .quantity(inventoryDto.getQuantity())
                .lastUpdated(inventoryDto.getLastUpdated())
                .build();
    }
}
