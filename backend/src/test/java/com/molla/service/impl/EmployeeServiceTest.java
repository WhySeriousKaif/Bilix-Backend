package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserService userService;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Branch branch;
    private User cashier2;
    private User kaif;
    private User bhumi;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(2L);
        Store store = new Store();
        store.setId(1L);
        branch.setStore(store);

        cashier2 = new User();
        cashier2.setId(20L);
        cashier2.setFullName("Cashier 2");
        cashier2.setRole(UserRole.ROLE_BRANCH_CASHIER);
        cashier2.setBranch(branch); // Set branch explicitly

        kaif = new User();
        kaif.setId(70L);
        kaif.setFullName("Kaif");
        kaif.setRole(UserRole.ROLE_BRANCH_CASHIER);
        kaif.setBranch(branch);

        bhumi = new User();
        bhumi.setId(80L);
        bhumi.setFullName("bhumi");
        bhumi.setRole(UserRole.ROLE_BRANCH_CASHIER);
        bhumi.setBranch(branch);
    }

    @Test
    void testFindBranchEmployees_ReturnsAllEmployees() throws Exception {
        // Arrange
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranchId(2L)).thenReturn(Arrays.asList(cashier2, kaif, bhumi));

        // Act
        List<UserDto> result = employeeService.findBranchEmployees(2L, null);

        // Assert
        assertEquals(3, result.size());
        assertEquals("Cashier 2", result.get(0).getFullName());
        assertEquals("Kaif", result.get(1).getFullName());
        assertEquals("bhumi", result.get(2).getFullName());
    }

    @Test
    void testFindBranchEmployees_FilterByRoleMismatch() throws Exception {
        // Arrange
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranchId(2L)).thenReturn(Arrays.asList(cashier2, kaif, bhumi));

        // Act - request ROLE_CASHIER, but users are ROLE_BRANCH_CASHIER
        // FIX: Now we expect 3 because we aliased it!
        List<UserDto> result = employeeService.findBranchEmployees(2L, UserRole.ROLE_CASHIER);

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    void testFindBranchEmployees_FilterByRoleMatch() throws Exception {
        // Arrange
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(userRepository.findByBranchId(2L)).thenReturn(Arrays.asList(cashier2, kaif, bhumi));

        // Act
        List<UserDto> result = employeeService.findBranchEmployees(2L, UserRole.ROLE_BRANCH_CASHIER);

        // Assert
        assertEquals(3, result.size());
    }
}
