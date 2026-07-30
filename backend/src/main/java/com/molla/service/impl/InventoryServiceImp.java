package com.molla.service.impl;

import com.molla.mapper.InventoryMapper;
import com.molla.model.Branch;
import com.molla.model.Inventory;
import com.molla.model.Product;
import com.molla.payload.dto.InventoryDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.InventoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.service.InventoryService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImp implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public InventoryDto createInventory(InventoryDto inventoryDto) {
        // Validate required fields
        if (inventoryDto.getBranchId() == null) {
            throw new RuntimeException("Branch ID is required");
        }
        if (inventoryDto.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        if (inventoryDto.getQuantity() == null) {
            throw new RuntimeException("Quantity is required");
        }
        accessControlService.requireBranchAccess(userService.getCurrentUser(), inventoryDto.getBranchId());

        Branch branch = branchRepository.findById(inventoryDto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Product product = productRepository.findById(inventoryDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Inventory inventory = InventoryMapper.toEntity(inventoryDto, branch, product);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDto(savedInventory);
    }

    @Override
    @CacheEvict(cacheNames = {"inventory", "inventoriesByBranch"}, allEntries = true)
    public InventoryDto updateInventory(InventoryDto inventoryDto, Long id) {
        Inventory existingInventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), existingInventory.getBranch().getId());
        existingInventory.setQuantity(inventoryDto.getQuantity());
        Inventory updatedInventory = inventoryRepository.save(existingInventory);
        return InventoryMapper.toDto(updatedInventory);
    }

    @Override
    @CacheEvict(cacheNames = {"inventory", "inventoriesByBranch"}, allEntries = true)
    public void deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), inventory.getBranch().getId());
        inventoryRepository.delete(inventory);
    }

    @Override
    @Cacheable(cacheNames = "inventory", key = "#productId + '_' + #branchId")
    public InventoryDto getInventoryByProductIdAndBranchId(Long productId, Long branchId) {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        Inventory inventory = inventoryRepository.findByProductIdAndBranchId(productId, branchId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    @Cacheable(cacheNames = "inventoriesByBranch", key = "#branchId")
    public List<InventoryDto> getAllInventoriesByBranchId(Long branchId) {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        return inventories.stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
