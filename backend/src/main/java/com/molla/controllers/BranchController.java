package com.molla.controllers;

import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.response.ApiResponse;
import com.molla.payload.dto.BranchDto;
import com.molla.service.BranchService;
import com.molla.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<BranchDto> createBranch(@RequestBody BranchDto branchDto,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        BranchDto createdBranch = branchService.createBranch(branchDto,user);

        return ResponseEntity.ok(createdBranch);
    }
    @GetMapping
    public ResponseEntity<List<BranchDto>> getAllBranches(@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(branchService.getAllBranches(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable("id") Long id,@RequestHeader(value = "Authorization", required = false) String jwt) throws UserException {
        BranchDto branch = branchService.getBranchById(id);
        return ResponseEntity.ok(branch);
    }
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<BranchDto>> getBranchesByStoreId(@PathVariable("storeId") Long storeId,@RequestHeader("Authorization") String jwt) throws UserException {
        List<BranchDto> branches = branchService.getBranchesByStoreId(storeId);
        return ResponseEntity.ok(branches);
    }
    @PutMapping("/{id}")
    public ResponseEntity<BranchDto> updateBranch(@PathVariable("id") Long id,@RequestBody BranchDto branchDto,@RequestHeader("Authorization") String jwt) throws UserException {
        User user = userService.getUserFromJwt(jwt);
        BranchDto updatedBranch = branchService.updateBranch(id,branchDto,user);
        return ResponseEntity.ok(updatedBranch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBranch(@PathVariable("id") Long id,@RequestHeader("Authorization") String jwt) throws UserException {
        branchService.deleteBranch(id);
        ApiResponse apiResponse=new ApiResponse("Branch deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
    
}
