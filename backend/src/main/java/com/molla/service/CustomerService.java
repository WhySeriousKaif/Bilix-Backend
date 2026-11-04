package com.molla.service;

import com.molla.model.Customer;
import com.molla.payload.dto.BranchCustomerDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CustomerService {

    Customer createCustomer(Customer customer);
    Customer updateCustomer(Customer customer, Long id);
    void deleteCustomer(Long id);
    Customer getCustomerById(Long id);
    List<Customer> getAllCustomers();
    Page<BranchCustomerDto> getBranchCustomers(Long branchId, String search, int page, int size);
    Page<Customer> getCustomersForCurrentUser(String search, Pageable pageable);
    List<Customer> searchCustomersForCurrentUser(String keyword);
}
