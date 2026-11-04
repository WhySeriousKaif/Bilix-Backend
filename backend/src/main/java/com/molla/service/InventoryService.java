package com.molla.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.molla.payload.dto.InventoryDto;

public interface InventoryService {

    InventoryDto createInventory(InventoryDto inventoryDto) ;
    InventoryDto updateInventory( InventoryDto inventoryDto, Long id) ;
    void deleteInventory(Long id) ;
    InventoryDto getInventoryByProductIdAndBranchId(Long productId, Long branchId) ;
    List<InventoryDto> getAllInventoriesByBranchId(Long branchId) ;
    Page<InventoryDto> getInventoriesByBranchPaged(Long branchId, Pageable pageable);

    
}
