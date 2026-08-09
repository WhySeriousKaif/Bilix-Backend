package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.UserException;
import com.molla.mapper.UserMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.EmployeeService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    @CacheEvict(cacheNames = { "users", "usersAll" }, allEntries = true)
    public UserDto createStoreEmployee(UserDto employee, Long storeId) throws UserException {
        accessControlService.requireManagerAccessToStore(userService.getCurrentUser(), storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found"));
        Branch branch = null; // branch is null if the employee is not a branch manager
        if (employee.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            if (employee.getBranchId() == null) {
                throw new UserException("Branch ID is required to create a branch manager");
            }
            branch = branchRepository.findById(employee.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        User user = UserMapper.toEntity(employee, passwordEncoder);
        user.setStore(store);
        user.setBranch(branch);

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == UserRole.ROLE_BRANCH_MANAGER && branch != null) {
            branch.setManager(savedUser);
            branchRepository.save(branch);
        }

        return UserMapper.toDto(savedUser);
    }

    @Override
    @CacheEvict(cacheNames = { "users", "usersAll" }, allEntries = true)
    public UserDto createBranchEmployee(UserDto employeeDetails, Long branchId) throws UserException {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        User currentUser = userService.getCurrentUser();
        accessControlService.requireManagerAccessToBranch(currentUser, branch);

        if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            User user = UserMapper.toEntity(employeeDetails, passwordEncoder);
            user.setBranch(branch);
            // Set store from branch
            user.setStore(branch.getStore());
            User savedUser = userRepository.save(user);
            if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
                branch.setManager(savedUser);
                branchRepository.save(branch);
            }
            return UserMapper.toDto(savedUser);
        }

        throw new UserException("Invalid role for branch employee");
    }

    @Override
    @CacheEvict(cacheNames = { "users", "usersAll" }, allEntries = true)
    public UserDto updateEmployee(UserDto employeeDetails, Long employeeId) throws UserException {
        User existingUser = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with this id"));

        // 🔐 Authorization: must have manager-level authority over this employee's current branch/store
        User currentUser = userService.getCurrentUser();
        if (existingUser.getBranch() != null) {
            accessControlService.requireManagerAccessToBranch(currentUser, existingUser.getBranch());
        } else if (existingUser.getStore() != null) {
            accessControlService.requireManagerAccessToStore(currentUser, existingUser.getStore().getId());
        } else {
            accessControlService.requireAdmin(currentUser);
        }

        // If moving the employee to a different branch, the caller needs authority there too
        if (employeeDetails.getBranchId() != null) {
            Branch targetBranch = branchRepository.findById(employeeDetails.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            accessControlService.requireManagerAccessToBranch(currentUser, targetBranch);
        }

        existingUser.setFullName(employeeDetails.getFullName());
        existingUser.setEmail(employeeDetails.getEmail());
        existingUser.setPhone(employeeDetails.getPhone());
        existingUser.setRole(employeeDetails.getRole());

        // Only update password if provided
        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }

        // Update branch if branchId is provided
        if (employeeDetails.getBranchId() != null) {
            Branch branch = branchRepository.findById(employeeDetails.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            existingUser.setBranch(branch);
            // Update store from branch
            existingUser.setStore(branch.getStore());
        }

        User savedUser = userRepository.save(existingUser);
        return UserMapper.toDto(savedUser);
    }

    @Override
    @CacheEvict(cacheNames = { "users", "usersAll" }, allEntries = true)
    public void deleteEmployee(Long employeeId) throws UserException {
        User existingUser = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        User currentUser = userService.getCurrentUser();
        if (currentUser.getId().equals(existingUser.getId())) {
            throw new UserException("You cannot delete your own account");
        }
        if (existingUser.getBranch() != null) {
            accessControlService.requireManagerAccessToBranch(currentUser, existingUser.getBranch());
        } else if (existingUser.getStore() != null) {
            accessControlService.requireManagerAccessToStore(currentUser, existingUser.getStore().getId());
        } else {
            accessControlService.requireAdmin(currentUser);
        }

        userRepository.delete(existingUser);
    }

    @Override
    public List<UserDto> findStoreEmployees(Long storeId, UserRole role) throws UserException {
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found"));
        return userRepository.findByStore(store)
                .stream()
                .filter(user -> role == null || user.getRole().equals(role)) //
                .map(UserMapper::toDto) // .map(user -> UserMapper.toDto(user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findBranchEmployees(Long branchId, UserRole role) throws UserException {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        List<User> employees = userRepository.findByBranchId(branchId);
        return employees.stream()
                .filter(user -> {
                    if (role == null)
                        return true;
                    if (user.getRole().equals(role))
                        return true;

                    // Alias: If searching for ROLE_CASHIER, also include ROLE_BRANCH_CASHIER
                    if (role == UserRole.ROLE_CASHIER && user.getRole() == UserRole.ROLE_BRANCH_CASHIER) {
                        return true;
                    }
                    // Alias: If searching for ROLE_BRANCH_MANAGER, also include ROLE_STORE_MANAGER?
                    // No, distinct roles.

                    return false;
                })
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }
}
