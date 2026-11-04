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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private static final Set<UserRole> STORE_CREATION_ROLES = Set.of(
            UserRole.ROLE_STORE_MANAGER, UserRole.ROLE_STORE_EMPLOYEE,
            UserRole.ROLE_BRANCH_MANAGER, UserRole.ROLE_BRANCH_CASHIER);

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

        if (!STORE_CREATION_ROLES.contains(employee.getRole())) {
            throw new BadRequestException("Invalid role for store employee");
        }
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
            if (branch.getStore() == null || !storeId.equals(branch.getStore().getId())) {
                throw new BadRequestException("Selected branch does not belong to this store");
            }
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

        UserRole nextRole = employeeDetails.getRole() != null ? employeeDetails.getRole() : existingUser.getRole();
        if (nextRole != existingUser.getRole()
                && (nextRole == UserRole.ROLE_ADMIN || nextRole == UserRole.ROLE_STORE_ADMIN)) {
            throw new BadRequestException("Cannot assign an admin role through employee management");
        }

        // Check the target branch even when a promotion omits branchId.
        Branch targetBranch = null;
        if (employeeDetails.getBranchId() != null) {
            targetBranch = branchRepository.findByIdForUpdate(employeeDetails.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            accessControlService.requireManagerAccessToBranch(currentUser, targetBranch);
        } else if (nextRole == UserRole.ROLE_BRANCH_MANAGER && previousBranch != null) {
            targetBranch = branchRepository.findByIdForUpdate(previousBranch.getId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        if (nextRole == UserRole.ROLE_BRANCH_MANAGER) {
            if (targetBranch == null) {
                throw new UserException("Branch ID is required for this role");
            }
            ensureBranchManagerAvailable(nextRole, targetBranch, existingUser);
        }

        existingUser.setFullName(employeeDetails.getFullName());
        existingUser.setEmail(employeeDetails.getEmail());
        existingUser.setPhone(employeeDetails.getPhone());
        existingUser.setRole(nextRole);

        // Only update password if provided
        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }

        if (employeeDetails.getBranchId() != null) {
            existingUser.setBranch(targetBranch);
            existingUser.setStore(targetBranch.getStore());
        }

        Branch newBranch = existingUser.getBranch();
        if (wasBranchManager && previousBranch != null
                && (nextRole != UserRole.ROLE_BRANCH_MANAGER || previousBranch != newBranch)) {
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
