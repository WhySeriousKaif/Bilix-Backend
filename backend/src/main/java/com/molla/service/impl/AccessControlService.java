package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Branch;
import com.molla.model.User;
import com.molla.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central place for "does this user actually have access to this resource" checks.
 *
 * Role checks alone (e.g. "is this a cashier?") aren't enough — a cashier or manager at
 * Branch A must not be able to read/modify data belonging to Branch B just by passing a
 * different id. Every controller/service that accepts a branchId/storeId/userId from the
 * client should call one of these before acting on it.
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final BranchRepository branchRepository;

    public void requireBranchAccess(User user, Long branchId) {
        if (branchId == null) {
            throw new BadRequestException("Branch id is required");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + branchId));

        if (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            Long userStoreId = user.getStore() != null ? user.getStore().getId() : null;
            Long branchStoreId = branch.getStore() != null ? branch.getStore().getId() : null;
            if (userStoreId != null && userStoreId.equals(branchStoreId)) {
                return;
            }
            throw new BadRequestException("You do not have access to this branch");
        }

        // Branch manager / cashier / branch cashier — must be assigned to this exact branch
        Long userBranchId = user.getBranch() != null ? user.getBranch().getId() : null;
        if (userBranchId != null && userBranchId.equals(branchId)) {
            return;
        }

        throw new BadRequestException("You do not have access to this branch");
    }

    public void requireStoreAccess(User user, Long storeId) {
        if (storeId == null) {
            throw new BadRequestException("Store id is required");
        }
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }

        Long userStoreId = user.getStore() != null
                ? user.getStore().getId()
                : (user.getBranch() != null && user.getBranch().getStore() != null
                        ? user.getBranch().getStore().getId()
                        : null);

        if (userStoreId != null && userStoreId.equals(storeId)) {
            return;
        }

        throw new BadRequestException("You do not have access to this store");
    }

    /**
     * For actions that manage staff (create/update/delete an employee) — stricter than
     * requireBranchAccess: a plain cashier at the right branch still must NOT pass this,
     * only admins/store admins/store managers/branch managers with authority over the branch.
     */
    public void requireManagerAccessToBranch(User user, Branch branch) {
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            Long userStoreId = user.getStore() != null ? user.getStore().getId() : null;
            Long branchStoreId = branch != null && branch.getStore() != null ? branch.getStore().getId() : null;
            if (userStoreId != null && userStoreId.equals(branchStoreId)) {
                return;
            }
            throw new BadRequestException("You do not have management access to this branch");
        }
        if (user.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            Long userBranchId = user.getBranch() != null ? user.getBranch().getId() : null;
            if (userBranchId != null && branch != null && userBranchId.equals(branch.getId())) {
                return;
            }
            throw new BadRequestException("You do not have management access to this branch");
        }
        throw new BadRequestException("Only a Store Admin, Store Manager, Branch Manager, or Super Admin can manage employees");
    }

    /** Store-level equivalent of requireManagerAccessToBranch, for actions not scoped to one branch. */
    public void requireManagerAccessToStore(User user, Long storeId) {
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            Long userStoreId = user.getStore() != null ? user.getStore().getId() : null;
            if (userStoreId != null && userStoreId.equals(storeId)) {
                return;
            }
            throw new BadRequestException("You do not have management access to this store");
        }
        throw new BadRequestException("Only a Store Admin, Store Manager, or Super Admin can manage employees at the store level");
    }

    /** For endpoints that should only ever be reachable by a super admin (system-wide views/actions). */
    public void requireAdmin(User user) {
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Only a Super Admin can perform this action");
        }
    }

    /** For endpoints scoped to "my own data" (e.g. a cashier's own shift/orders). */
    public void requireSelfOrAdmin(User user, Long targetUserId) {
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return;
        }
        if (user.getId() != null && user.getId().equals(targetUserId)) {
            return;
        }
        throw new BadRequestException("You do not have access to this resource");
    }
}
