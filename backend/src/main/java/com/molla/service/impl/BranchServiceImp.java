package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.service.BranchService;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.BranchMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchServiceImp implements BranchService {
    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public BranchDto createBranch(BranchDto branchDto, User user) {
        User currentUser = userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());
        if (store == null) {
            throw new NotFoundException("Only a Store Admin can create a branch for their store");
        }

        Branch branch = BranchMapper.toEntity(branchDto, store);
        Branch savedBranch = branchRepository.save(branch);
        return BranchMapper.toDto(savedBranch);
    }

    @Override
    @Cacheable(cacheNames = "branches", key = "#id")
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), id);
        return BranchMapper.toDto(branch);
    }

    @Override
    @Cacheable(cacheNames = "branchesByStore", key = "#storeId")
    public List<BranchDto> getBranchesByStoreId(Long storeId) {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        List<Branch> branches = branchRepository.findByStoreId(storeId);
        return branches.stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<BranchDto> getAllBranches(User user) {
        if (user.getRole().equals(UserRole.ROLE_ADMIN)) {
            // Super admin: return all branches
            return branchRepository.findAll().stream()
                    .map(BranchMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            // Store admin: return only branches in their store
            Store store = storeRepository.findByStoreAdminId(user.getId());
            if (store == null) {
                throw new NotFoundException("Store not found for this admin. Please create a store first.");
            }
            return getBranchesByStoreId(store.getId());
        }
    }

    @Override
    @CacheEvict(cacheNames = {"branches", "branchesByStore"}, allEntries = true)
    public BranchDto updateBranch(Long id, BranchDto branchDto, User user) {
        Branch existingBranch = branchRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        accessControlService.requireBranchInfoEditAccess(userService.getCurrentUser(), existingBranch);
        // Partial update: only overwrite fields actually supplied, so callers that
        // only send a subset (e.g. name/address/phone/email) don't wipe the rest.
        if (branchDto.getName() != null) existingBranch.setName(branchDto.getName());
        if (branchDto.getPhone() != null) existingBranch.setPhone(branchDto.getPhone());
        if (branchDto.getAddress() != null) existingBranch.setAddress(branchDto.getAddress());
        if (branchDto.getEmail() != null) existingBranch.setEmail(branchDto.getEmail());
        if (branchDto.getWorkingDays() != null) existingBranch.setWorkingDays(branchDto.getWorkingDays());
        if (branchDto.getOpenTime() != null) existingBranch.setOpenTime(branchDto.getOpenTime());
        if (branchDto.getCloseTime() != null) existingBranch.setCloseTime(branchDto.getCloseTime());
        Branch savedBranch = branchRepository.save(existingBranch);
        return BranchMapper.toDto(savedBranch);

    }

    @Override
    @CacheEvict(cacheNames = {"branches", "branchesByStore"}, allEntries = true)
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        accessControlService.requireManagerAccessToBranch(userService.getCurrentUser(), branch);
        branchRepository.delete(branch);
    }
}