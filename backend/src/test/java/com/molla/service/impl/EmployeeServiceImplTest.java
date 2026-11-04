package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.UserException;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock StoreRepository storeRepository;
    @Mock BranchRepository branchRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserService userService;
    @Mock AccessControlService accessControlService;
    @InjectMocks EmployeeServiceImpl service;

    @Test
    void storeEmployeeCannotBeAssignedToAnotherStoresBranch() {
        Store requestedStore = store(10L);
        Branch otherBranch = branch(20L, store(11L));
        UserDto request = new UserDto();
        request.setEmail("employee@example.com");
        request.setRole(UserRole.ROLE_BRANCH_CASHIER);
        request.setBranchId(20L);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(requestedStore));
        when(branchRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(otherBranch));

        assertThrows(BadRequestException.class, () -> service.createStoreEmployee(request, 10L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void storeEmployeeCreationRejectsUnintendedRole() {
        UserDto request = new UserDto();
        request.setRole(UserRole.ROLE_ADMIN);

        assertThrows(BadRequestException.class, () -> service.createStoreEmployee(request, 10L));
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void managerPromotionWithoutBranchIdCannotReplaceExistingManager() {
        Store store = store(10L);
        Branch branch = branch(20L, store);
        User assignedManager = new User();
        assignedManager.setId(30L);
        branch.setManager(assignedManager);
        User employee = new User();
        employee.setId(40L);
        employee.setRole(UserRole.ROLE_BRANCH_CASHIER);
        employee.setBranch(branch);
        employee.setStore(store);
        when(userRepository.findById(40L)).thenReturn(Optional.of(employee));
        when(branchRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(branch));
        UserDto request = new UserDto();
        request.setRole(UserRole.ROLE_BRANCH_MANAGER);

        assertThrows(UserException.class, () -> service.updateEmployee(request, 40L));
        assertEquals(30L, branch.getManager().getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    void employeeCannotBePromotedToAdminThroughUpdate() {
        User employee = new User();
        employee.setId(40L);
        employee.setRole(UserRole.ROLE_STORE_EMPLOYEE);
        employee.setStore(store(10L));
        when(userRepository.findById(40L)).thenReturn(Optional.of(employee));
        UserDto request = new UserDto();
        request.setRole(UserRole.ROLE_ADMIN);

        assertThrows(BadRequestException.class, () -> service.updateEmployee(request, 40L));
        verify(userRepository, never()).save(any());
    }

    private Store store(Long id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private Branch branch(Long id, Store store) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setStore(store);
        return branch;
    }
}
