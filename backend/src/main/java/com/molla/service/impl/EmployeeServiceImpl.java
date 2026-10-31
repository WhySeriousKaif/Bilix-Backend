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
import org.springframework.transaction.annotation.Transactional;

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
    @CacheEvict(cacheNames = { "users", "usersAll", "branches", "branchesByStore" }, allEntries = true)
    @Transactional
    public UserDto createStoreEmployee(UserDto employee, Long storeId) throws UserException {
        accessControlService.requireManagerAccessToStore(userService.getCurrentUser(), storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found"));
        Branch branch = null; // branch stays null for store-level roles (Store Manager, Store Employee)
        boolean isBranchScopedRole = employee.getRole() == UserRole.ROLE_BRANCH_MANAGER
                || employee.getRole() == UserRole.ROLE_BRANCH_CASHIER;
        if (isBranchScopedRole) {
            if (employee.getBranchId() == null) {
                throw new UserException("Branch ID is required for this role");
            }
            branch = branchRepository.findByIdForUpdate(employee.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            ensureBranchManagerAvailable(employee.getRole(), branch, null);
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
    @CacheEvict(cacheNames = { "users", "usersAll", "branches", "branchesByStore" }, allEntries = true)
    @Transactional
    public UserDto createBranchEmployee(UserDto employeeDetails, Long branchId) throws UserException {
        Branch branch = branchRepository.findByIdForUpdate(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        User currentUser = userService.getCurrentUser();
        accessControlService.requireManagerAccessToBranch(currentUser, branch);

        if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            ensureBranchManagerAvailable(employeeDetails.getRole(), branch, null);
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
    @CacheEvict(cacheNames = { "users", "usersAll", "branches", "branchesByStore" }, allEntries = true)
    @Transactional
    public UserDto updateEmployee(UserDto employeeDetails, Long employeeId) throws UserException {
        User existingUser = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with this id"));

        // 🔐 Authorization: must have manager-level authority over this employee's current branch/store
        User currentUser = userService.getCurrentUser();
        Branch previousBranch = existingUser.getBranch();
        boolean wasBranchManager = existingUser.getRole() == UserRole.ROLE_BRANCH_MANAGER;
        if (existingUser.getBranch() != null) {
            accessControlService.requireManagerAccessToBranch(currentUser, existingUser.getBranch());
        } else if (existingUser.getStore() != null) {
            accessControlService.requireManagerAccessToStore(currentUser, existingUser.getStore().getId());
        } else {
            accessControlService.requireAdmin(currentUser);
        }

        // If moving the employee to a different branch, the caller needs authority there too
        if (employeeDetails.getBranchId() != null) {
            Branch targetBranch = branchRepository.findByIdForUpdate(employeeDetails.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            accessControlService.requireManagerAccessToBranch(currentUser, targetBranch);
            ensureBranchManagerAvailable(employeeDetails.getRole(), targetBranch, existingUser);
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

        Branch newBranch = existingUser.getBranch();
        if (wasBranchManager && previousBranch != null
                && (employeeDetails.getRole() != UserRole.ROLE_BRANCH_MANAGER || previousBranch != newBranch)) {
            if (previousBranch.getManager() != null && previousBranch.getManager().getId().equals(existingUser.getId())) {
                previousBranch.setManager(null);
                branchRepository.save(previousBranch);
            }
        }

        User savedUser = userRepository.save(existingUser);
        if (savedUser.getRole() == UserRole.ROLE_BRANCH_MANAGER && savedUser.getBranch() != null) {
            savedUser.getBranch().setManager(savedUser);
            branchRepository.save(savedUser.getBranch());
        }
        return UserMapper.toDto(savedUser);
    }

    private void ensureBranchManagerAvailable(UserRole role, Branch branch, User currentEmployee) throws UserException {
        if (role != UserRole.ROLE_BRANCH_MANAGER || branch.getManager() == null) {
            return;
        }
        if (currentEmployee != null && branch.getManager().getId().equals(currentEmployee.getId())) {
            return;
        }
        throw new UserException("This branch already has a manager. Edit or remove the current manager before assigning another.");
    }

    @Override
    @CacheEvict(cacheNames = { "users", "usersAll", "branches", "branchesByStore" }, allEntries = true)
    @Transactional
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

        // Clear the owning branch relationship before deleting a manager. This
        // also allows removal of stale manager accounts after a replacement.
        if (existingUser.getBranch() != null) {
            Branch branch = branchRepository.findByIdForUpdate(existingUser.getBranch().getId())
                    .orElse(null);
            if (branch != null && branch.getManager() != null
                    && branch.getManager().getId().equals(existingUser.getId())) {
                branch.setManager(null);
                branchRepository.save(branch);
            }
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
