package com.molla.service.impl;

import com.molla.domain.StoreStatus;
import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;
import com.molla.repository.StoreRepository;
import com.molla.service.EmployeeService;
import com.molla.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImpTest {
    @Mock StoreRepository storeRepository;
    @Mock UserService userService;
    @Mock EmployeeService employeeService;
    @Mock AccessControlService accessControlService;
    @InjectMocks StoreServiceImp service;

    @Test
    void storeReadChecksCallerBeforeRepositoryLookup() {
        User caller = new User();
        when(userService.getCurrentUser()).thenReturn(caller);
        doThrow(new BadRequestException("Forbidden"))
                .when(accessControlService).requireStoreAccess(caller, 20L);

        assertThrows(BadRequestException.class, () -> service.getStoreById(20L));
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void creationIgnoresClientIdentityStatusAndTimestamps() {
        User admin = new User();
        admin.setId(7L);
        admin.setRole(UserRole.ROLE_STORE_ADMIN);
        StoreDto request = new StoreDto();
        request.setId(99L);
        request.setBrand("My Store");
        request.setStoreStatus(StoreStatus.ACTIVE);
        request.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            assertNull(store.getId());
            assertNull(store.getCreatedAt());
            assertEquals(StoreStatus.PENDING, store.getStoreStatus());
            store.setId(10L);
            return store;
        });

        StoreDto result = service.createStore(request, admin);

        assertEquals(10L, result.getId());
        assertEquals(StoreStatus.PENDING, result.getStoreStatus());
        assertEquals(10L, admin.getStore().getId());
        verify(userService).updateUser(admin);
    }

    @Test
    void updatePreservesExistingValuesWhenOmitted() {
        User superAdmin = new User();
        superAdmin.setRole(UserRole.ROLE_ADMIN);
        Store existing = new Store();
        existing.setId(10L);
        existing.setBrand("Original");
        existing.setDescription("Description");
        when(userService.getCurrentUser()).thenReturn(superAdmin);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(existing)).thenReturn(existing);

        StoreDto result = service.updateStore(10L, new StoreDto());

        assertEquals("Original", result.getBrand());
        assertEquals("Description", result.getDescription());
    }
}
