package com.molla.service.impl;

import com.molla.domain.StoreStatus;
import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.exceptions.UserException;
import com.molla.mapper.StoreMapper;
import com.molla.model.Store;
import com.molla.model.StoreContact;
import com.molla.model.User;
import com.molla.payload.dto.StoreDto;
import com.molla.payload.dto.UserDto;
import com.molla.repository.StoreRepository;
import com.molla.service.EmployeeService;
import com.molla.service.StoreService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreServiceImp implements StoreService {

    private final StoreRepository storeRepository;
    private final UserService userService;
    private final EmployeeService employeeService;
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = { "stores", "storesAll", "storesByAdmin" }, allEntries = true)
    public StoreDto createStore(StoreDto storeDto, User user) {
        // 🔐 Authorization: Only a Store Admin may create/register their own store
        if (!user.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
            throw new BadRequestException("Only a Store Admin can create a store");
        }

        // Check if user already has a store
        Store existingStore = storeRepository.findByStoreAdminId(user.getId());
        if (existingStore != null) {
            throw new BadRequestException("User already has a store. One user can only have one store.");
        }

        Store store = StoreMapper.toEntity(storeDto, user);
        Store savedStore = storeRepository.save(store);

        // Update user to link to this store
        user.setStore(savedStore);
        userService.updateUser(user); // Assuming updateUser method exists or use repository

        return StoreMapper.toDTO(savedStore);
    }

    @Override
    public StoreDto getStoreById(Long id) {
        // This check must run on every request. A shared cache keyed only by store ID
        // could otherwise return a DTO without re-checking the current caller.
        accessControlService.requireStoreAccess(userService.getCurrentUser(), id);
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + id));
        return StoreMapper.toDTO(store);
    }

    @Override
    public List<StoreDto> getAllStores() {
        accessControlService.requireAdmin(userService.getCurrentUser());
        List<Store> dtos = storeRepository.findAll();
        return dtos.stream().map(StoreMapper::toDTO).collect(Collectors.toList());

    }

    @Override
    @Cacheable(cacheNames = "storesByAdmin", key = "#user.id")
    public StoreDto getStoreByAdmin(User user) {
        Store store = storeRepository.findByStoreAdminId(user.getId());
        if (store == null) {
            throw new NotFoundException("Store not found for this admin. Please create a store first.");
        }
        return StoreMapper.toDTO(store);
    }

    @Override
    public List<StoreDto> getStoresForAdmin(User user) {
        if (user.getRole().equals(UserRole.ROLE_ADMIN)) {
            return getAllStores();
        }
        return List.of(getStoreByAdmin(user));
    }

    @Override
    @CacheEvict(cacheNames = { "stores", "storesByAdmin", "storesAll" }, allEntries = true)
    public StoreDto updateStore(Long id, StoreDto storeDto) {
        User currentUser = userService.getCurrentUser();
        Store existingStore = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + id));

        // 🔐 Authorization: Super admin can update any store; store admin can only
        // update their own store
        if (currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            // Super admin: allow updating any store

        } else if (currentUser.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
            // Store admin: only allow updating their own store
            Store userStore = storeRepository.findByStoreAdminId(currentUser.getId());

            if (userStore == null || !userStore.getId().equals(id)) {
                throw new BadRequestException("You can only update your own store");
            }
        } else {
            throw new BadRequestException("Only Super Admin or Store Admin can update stores");
        }

        if (storeDto.getBrand() != null) {
            if (storeDto.getBrand().isBlank()) {
                throw new BadRequestException("Store brand is required");
            }
            existingStore.setBrand(storeDto.getBrand());
        }
        if (storeDto.getDescription() != null) {
            existingStore.setDescription(storeDto.getDescription());
        }
        if (storeDto.getStoreType() != null) {
            existingStore.setStoreType(storeDto.getStoreType());
        }
        if (storeDto.getContact() != null) {
            StoreContact contact = StoreContact.builder()
                    .address(storeDto.getContact().getAddress())
                    .phone(storeDto.getContact().getPhone())
                    .email(storeDto.getContact().getEmail())
                    .build();
            existingStore.setContact(contact);
        }
        return StoreMapper.toDTO(storeRepository.save(existingStore));
    }

    @Override
    @CacheEvict(cacheNames = { "stores", "storesByAdmin", "storesAll" }, allEntries = true)
    public void deleteStore(Long id) {
        User currentUser = userService.getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new BadRequestException("Only Super Admin can delete stores");
        }
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + id));
        storeRepository.delete(store);
    }

    @Override
    public StoreDto getStoreByEmployee() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new BadRequestException("You don't have permission to access this store");
        }
        Store store = currentUser.getStore();
        if (store == null) {
            throw new NotFoundException("Store not found for this employee");
        }
        return StoreMapper.toDTO(store);
    }

    @Override
    @CacheEvict(cacheNames = { "stores", "storesByAdmin", "storesAll" }, allEntries = true)
    public StoreDto moderateStore(Long id, StoreStatus storeStatus) {
        // 🔐 Authorization: Only Super Admin (ROLE_ADMIN) can moderate stores
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new BadRequestException("Only Super Admin can moderate stores");
        }

        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store not found with id: " + id));
        store.setStoreStatus(storeStatus);
        return StoreMapper.toDTO(storeRepository.save(store));
    }

    @Override
    public StoreDto getMyStore(User user) {
        // Get store admin's own store
        return getStoreByAdmin(user);
    }

    @Override
    public List<UserDto> getMyStoreEmployees(User user) throws UserException {
        // Get store admin's store
        Store store = storeRepository.findByStoreAdminId(user.getId());
        if (store == null) {
            throw new NotFoundException("Store not found for this admin. Please create a store first.");
        }
        // Return all employees in this store (cashiers, employees, etc.)
        return employeeService.findStoreEmployees(store.getId(), null);
    }
}
