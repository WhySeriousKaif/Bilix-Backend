package com.molla.controllers;

import com.molla.payload.response.ApiResponse;
import com.molla.payload.dto.BranchDto;
import com.molla.service.BranchService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<BranchDto> createBranch(@Valid @RequestBody BranchDto branchDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(branchDto));
    }

    @GetMapping
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<BranchDto>> getBranchesByStoreId(@PathVariable("storeId") Long storeId) {
        return ResponseEntity.ok(branchService.getBranchesByStoreId(storeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDto> updateBranch(
            @PathVariable("id") Long id,
            @RequestBody BranchDto branchDto) {
        return ResponseEntity.ok(branchService.updateBranch(id, branchDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBranch(@PathVariable("id") Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(new ApiResponse("Branch deleted successfully"));
    }
    
}
