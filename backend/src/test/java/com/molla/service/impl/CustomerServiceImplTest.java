package com.molla.service.impl;

import com.molla.model.Branch;
import com.molla.model.Customer;
import com.molla.model.User;
import com.molla.repository.CustomerRepository;
import com.molla.repository.OrderRepository;
import com.molla.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void branchCustomerPagingNormalizesSearchAndPageLimits() {
        User user = branchUser(4L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.findBranchCustomers(any(), any(), any())).thenReturn(Page.empty());

        customerService.getBranchCustomers(4L, "  alice  ", -2, 500);

        verify(accessControlService).requireBranchAccess(user, 4L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findBranchCustomers(
                org.mockito.ArgumentMatchers.eq(4L),
                org.mockito.ArgumentMatchers.eq("alice"),
                pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void pagedCustomersAreScopedToCurrentUsersBranch() {
        when(userService.getCurrentUser()).thenReturn(branchUser(4L));
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findByBranchIdAndSearch(4L, "alice", pageable))
                .thenReturn(Page.empty());

        customerService.getCustomersForCurrentUser(" alice ", pageable);

        verify(customerRepository).findByBranchIdAndSearch(4L, "alice", pageable);
    }

    @Test
    void customerLookupRejectsARecordOutsideTheUsersBranch() {
        when(userService.getCurrentUser()).thenReturn(branchUser(4L));
        when(orderRepository.existsByCustomerIdAndBranchId(9L, 4L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> customerService.getCustomerById(9L));
    }

    @Test
    void customerUpdatePreservesTheExistingEntityAndChecksBranchAccess() {
        User user = branchUser(4L);
        Customer existing = new Customer();
        existing.setId(9L);
        existing.setName("Old Name");
        Customer changes = new Customer();
        changes.setName("New Name");
        changes.setEmail("new@example.com");
        changes.setPhone("1234567890");

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderRepository.existsByCustomerIdAndBranchId(9L, 4L)).thenReturn(true);
        when(customerRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        Customer result = customerService.updateCustomer(changes, 9L);

        verify(accessControlService).requireBranchAccess(user, 4L);
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
    }

    private User branchUser(Long branchId) {
        Branch branch = new Branch();
        branch.setId(branchId);
        User user = new User();
        user.setId(1L);
        user.setBranch(branch);
        return user;
    }
}
