package com.molla.service;

import com.molla.payload.dto.BranchDto;

import java.util.List;

public interface BranchService {

    BranchDto createBranch(BranchDto branchDto);
    BranchDto getBranchById(Long id);
    List<BranchDto> getBranchesByStoreId(Long storeId);
    /** Super admin: all branches; store admin: branches in their store only. */
    List<BranchDto> getAllBranches();
    BranchDto updateBranch(Long id, BranchDto branchDto);
    void deleteBranch(Long id);
}
