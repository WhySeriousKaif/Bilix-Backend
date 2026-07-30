package com.molla.service;

import com.molla.model.User;
import com.molla.payload.dto.BranchDto;

import java.util.List;

public interface BranchService {

    BranchDto createBranch(BranchDto branchDto, User user);
    BranchDto getBranchById(Long id);
    List<BranchDto> getBranchesByStoreId(Long storeId);
    /** Super admin: all branches; store admin: branches in their store only. */
    List<BranchDto> getAllBranches(User user);
    BranchDto updateBranch(Long id, BranchDto branchDto, User user);
    void deleteBranch(Long id);
}
