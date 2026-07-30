package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.payload.dto.InventoryDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.InventoryService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<InventoryDto> createInventory(@RequestBody InventoryDto inventoryDto) throws UserException {
        return ResponseEntity.ok(inventoryService.createInventory(inventoryDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryDto> updateInventory(@PathVariable("id") Long id,
                                                        @RequestBody InventoryDto inventoryDto) throws UserException {
        return ResponseEntity.ok(inventoryService.updateInventory(inventoryDto, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInventory(@PathVariable("id") Long id) throws UserException {
        inventoryService.deleteInventory(id);
        ApiResponse apiResponse = new ApiResponse("Inventory deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/product/{productId}/branch/{branchId}")
    public ResponseEntity<InventoryDto> getInventoryByProductIdAndBranchId(@PathVariable("productId") Long productId,
                                                                           @PathVariable("branchId") Long branchId) throws UserException {
        return ResponseEntity.ok(inventoryService.getInventoryByProductIdAndBranchId(productId, branchId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryDto>> getInventoriesByBranchId(@PathVariable("branchId") Long branchId) throws UserException {
        return ResponseEntity.ok(inventoryService.getAllInventoriesByBranchId(branchId));
    }
}
