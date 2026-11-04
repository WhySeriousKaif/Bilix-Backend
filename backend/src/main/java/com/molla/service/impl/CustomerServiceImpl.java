package com.molla.service.impl;

import com.molla.model.Customer;
import com.molla.model.User;
import com.molla.payload.dto.BranchCustomerDto;
import com.molla.service.CustomerService;
import com.molla.service.UserService;
import com.molla.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.molla.repository.CustomerRepository;  
import com.molla.repository.OrderRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;

    @Override
    public Customer createCustomer(Customer customer) {
        // Ignore id if provided - it should be auto-generated
        customer.setId(null);
        customer.setCreatedAt(java.time.LocalDateTime.now());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Customer customer, Long id) {
        requireCustomerBranchAccess(id);
        Customer customerToUpdate = findCustomer(id);
        customerToUpdate.setName(customer.getName());
        customerToUpdate.setEmail(customer.getEmail());
        customerToUpdate.setPhone(customer.getPhone());

        return customerRepository.save(customerToUpdate);
    }

    @Override
    public void deleteCustomer(Long id) {
        requireCustomerBranchAccess(id);
        customerRepository.delete(findCustomer(id));
    }

    @Override
    public Customer getCustomerById(Long id) {
        requireCustomerBranchAccess(id);
        return findCustomer(id);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Page<BranchCustomerDto> getBranchCustomers(
            Long branchId, String search, int page, int size) {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        String term = normalizeSearch(search);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        return orderRepository.findBranchCustomers(branchId, term, pageable);
    }

    @Override
    public Page<Customer> getCustomersForCurrentUser(String search, Pageable pageable) {
        User user = userService.getCurrentUser();
        String term = normalizeSearch(search);
        if (user.getBranch() != null) {
            return term == null
                    ? customerRepository.findByBranchId(user.getBranch().getId(), pageable)
                    : customerRepository.findByBranchIdAndSearch(user.getBranch().getId(), term, pageable);
        }
        return term == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
    }

    @Override
    public List<Customer> searchCustomersForCurrentUser(String keyword) {
        User user = userService.getCurrentUser();
        if (user.getBranch() != null) {
            return customerRepository.findByBranchIdAndSearch(
                    user.getBranch().getId(), keyword, PageRequest.of(0, 50)).getContent();
        }
        return customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));
    }

    private void requireCustomerBranchAccess(Long customerId) {
        User user = userService.getCurrentUser();
        if (user.getBranch() == null
                || !orderRepository.existsByCustomerIdAndBranchId(customerId, user.getBranch().getId())) {
            throw new AccessDeniedException("Customer is outside your branch");
        }
        accessControlService.requireBranchAccess(user, user.getBranch().getId());
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
