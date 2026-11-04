package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private BranchRepository branchRepository;

    private AccessControlService accessControlService;
    private Branch branchA;
    private Branch branchB;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(branchRepository);

        Store storeA = new Store();
        storeA.setId(10L);
        Store storeB = new Store();
        storeB.setId(20L);

        branchA = new Branch();
        branchA.setId(1L);
        branchA.setStore(storeA);

        branchB = new Branch();
        branchB.setId(2L);
        branchB.setStore(storeB);

        lenient().when(branchRepository.findById(1L)).thenReturn(Optional.of(branchA));
        lenient().when(branchRepository.findById(2L)).thenReturn(Optional.of(branchB));
    }

    @Test
    void branchManagerCanAccessAssignedBranch() {
        User manager = user(100L, UserRole.ROLE_BRANCH_MANAGER, branchA, branchA.getStore());

        assertDoesNotThrow(() -> accessControlService.requireBranchAccess(manager, 1L));
    }

    @Test
    void branchManagerCannotAccessAnotherBranch() {
        User manager = user(100L, UserRole.ROLE_BRANCH_MANAGER, branchA, branchA.getStore());

        assertThrows(BadRequestException.class,
                () -> accessControlService.requireBranchAccess(manager, 2L));
    }

    @Test
    void storeManagerCanAccessBranchInOwnStore() {
        User manager = user(200L, UserRole.ROLE_STORE_MANAGER, null, branchA.getStore());

        assertDoesNotThrow(() -> accessControlService.requireBranchAccess(manager, 1L));
    }

    @Test
    void storeManagerCannotAccessAnotherStore() {
        User manager = user(200L, UserRole.ROLE_STORE_MANAGER, null, branchA.getStore());

        assertThrows(BadRequestException.class,
                () -> accessControlService.requireBranchAccess(manager, 2L));
    }

    @Test
    void cashierCannotManageEmployees() {
        User cashier = user(300L, UserRole.ROLE_CASHIER, branchA, branchA.getStore());

        assertThrows(BadRequestException.class,
                () -> accessControlService.requireManagerAccessToBranch(cashier, branchA));
    }

    @Test
    void userCanAccessOwnUserRecord() {
        User current = user(400L, UserRole.ROLE_CASHIER, branchA, branchA.getStore());

        assertDoesNotThrow(() -> accessControlService.requireUserAccess(current, current));
    }

    @Test
    void userCannotAccessAnotherBranchUser() {
        User current = user(400L, UserRole.ROLE_CASHIER, branchA, branchA.getStore());
        User target = user(401L, UserRole.ROLE_CASHIER, branchB, branchB.getStore());

        assertThrows(BadRequestException.class,
                () -> accessControlService.requireUserAccess(current, target));
    }

    private User user(Long id, UserRole role, Branch branch, Store store) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setBranch(branch);
        user.setStore(store);
        return user;
    }
}
