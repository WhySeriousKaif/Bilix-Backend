package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.mapper.BranchMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceImpTest {
    @Mock BranchRepository branchRepository;
    @Mock StoreRepository storeRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock AccessControlService accessControlService;
    @InjectMocks BranchServiceImp service;

    @Test
    void branchReadChecksAccessBeforeReturningData() {
        User cashier = new User();
        when(userService.getCurrentUser()).thenReturn(cashier);
        doThrow(new BadRequestException("Forbidden"))
                .when(accessControlService).requireBranchAccess(cashier, 4L);

        assertThrows(BadRequestException.class, () -> service.getBranchById(4L));
        verify(branchRepository, never()).findById(any());
    }

    @Test
    void branchStaffListContainsOnlyAssignedBranch() {
        Branch branch = new Branch();
        branch.setId(4L);
        branch.setName("Assigned branch");
        User manager = new User();
        manager.setRole(UserRole.ROLE_BRANCH_MANAGER);
        manager.setBranch(branch);
        when(userService.getCurrentUser()).thenReturn(manager);
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));

        List<BranchDto> result = service.getAllBranches();

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
        verify(accessControlService).requireBranchAccess(manager, 4L);
        verify(branchRepository, never()).findAll();
    }

    @Test
    void storeAdminListUsesStoreTheyAdminister() {
        User admin = new User();
        admin.setId(7L);
        admin.setRole(UserRole.ROLE_STORE_ADMIN);
        Store store = new Store();
        store.setId(8L);
        Branch branch = new Branch();
        branch.setId(4L);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(storeRepository.findByStoreAdminId(7L)).thenReturn(store);
        when(branchRepository.findByStoreId(8L)).thenReturn(List.of(branch));

        List<BranchDto> result = service.getAllBranches();

        assertEquals(1, result.size());
        verify(accessControlService).requireStoreAccess(admin, 8L);
        verify(branchRepository, never()).findAll();
    }

    @Test
    void cashierCannotCreateBranch() {
        User cashier = new User();
        cashier.setRole(UserRole.ROLE_CASHIER);
        when(userService.getCurrentUser()).thenReturn(cashier);

        assertThrows(BadRequestException.class, () -> service.createBranch(new BranchDto()));
        verify(storeRepository, never()).findByStoreAdminId(any());
    }

    @Test
    void creationIgnoresClientIdentityAndTimestamps() {
        Store store = new Store();
        store.setId(8L);
        BranchDto request = new BranchDto();
        request.setId(99L);
        request.setName("New branch");
        request.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        request.setUpdatedAt(LocalDateTime.of(2020, 1, 2, 0, 0));

        Branch result = BranchMapper.toEntity(request, store);

        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
        assertEquals(store, result.getStore());
    }

    @Test
    void deletingBranchWithEmployeesDoesNotDeleteTheirAccounts() {
        Branch branch = new Branch();
        branch.setId(4L);
        User employee = new User();
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranchId(4L)).thenReturn(List.of(employee));

        assertThrows(BadRequestException.class, () -> service.deleteBranch(4L));
        verify(branchRepository, never()).delete(any());
    }
}
