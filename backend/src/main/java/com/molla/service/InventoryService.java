package com.molla.service;

import java.util.List;

import com.molla.payload.dto.InventoryDto;

public interface InventoryService {

    InventoryDto createInventory(InventoryDto inventoryDto) ;
    InventoryDto updateInventory( InventoryDto inventoryDto, Long id) ;
    void deleteInventory(Long id) ;
    InventoryDto getInventoryByProductIdAndBranchId(Long productId, Long branchId) ;
    List<InventoryDto> getAllInventoriesByBranchId(Long branchId) ;

    
}
