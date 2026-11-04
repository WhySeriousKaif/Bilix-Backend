package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.service.BranchService;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.BranchMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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
    private final UserRepository userRepository;

    @Override
    @CacheEvict(cacheNames = { "branches", "branchesByStore" }, allEntries = true)
    public BranchDto createBranch(BranchDto branchDto) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new BadRequestException("Only a Store Admin can create a branch for their store");
        }
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());
        if (store == null) {
            throw new NotFoundException("Only a Store Admin can create a branch for their store");
        }

        Branch branch = BranchMapper.toEntity(branchDto, store);
        Branch savedBranch = branchRepository.save(branch);
        return BranchMapper.toDto(savedBranch);
    }

    @Override
    public BranchDto getBranchById(Long id) {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), id);
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        return BranchMapper.toDto(branch);
    }

    @Override
    public List<BranchDto> getBranchesByStoreId(Long storeId) {

        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);

        List<Branch> branches = branchRepository.findByStoreId(storeId);
        return branches.stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<BranchDto> getAllBranches() {
        User user = userService.getCurrentUser();
        UserRole role = user.getRole();
        if (role == UserRole.ROLE_ADMIN) {
            // Super admin: return all branches
            return branchRepository.findAll().stream()
                    .map(BranchMapper::toDto)
                    .collect(Collectors.toList());
        }
        if (role == UserRole.ROLE_STORE_ADMIN || role == UserRole.ROLE_STORE_MANAGER) {
            Store store = role == UserRole.ROLE_STORE_ADMIN
                    ? storeRepository.findByStoreAdminId(user.getId())
                    : user.getStore();
            if (store == null) {
                throw new NotFoundException("Store not found for this user.");
            }
            return getBranchesByStoreId(store.getId());
        }
        if (role == UserRole.ROLE_BRANCH_MANAGER || role == UserRole.ROLE_BRANCH_CASHIER
                || role == UserRole.ROLE_CASHIER) {
            Branch branch = user.getBranch();
            if (branch == null) {
                throw new NotFoundException("No branch is assigned to this user.");
            }
            return List.of(getBranchById(branch.getId()));
        }
        throw new BadRequestException("You do not have access to branches");
    }

    @Override
    @CacheEvict(cacheNames = {"branches", "branchesByStore"}, allEntries = true)
    public BranchDto updateBranch(Long id, BranchDto branchDto) {
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
        if (!userRepository.findByBranchId(id).isEmpty()) {
            throw new BadRequestException("Reassign branch employees before deleting this branch");
        }
        branchRepository.delete(branch);
    }
}
