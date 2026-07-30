package com.molla.service.impl;

import com.molla.model.Customer;
import com.molla.service.CustomerService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.molla.repository.CustomerRepository;  
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService{
    private final CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer) {
        // Ignore id if provided - it should be auto-generated
        customer.setId(null);
        customer.setCreatedAt(java.time.LocalDateTime.now());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Customer customer, Long id) throws Exception {
         Customer customerToUpdate=customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
         customerToUpdate.setName(customer.getName());
         customerToUpdate.setEmail(customer.getEmail());
         customerToUpdate.setPhone(customer.getPhone());

         return customerRepository.save(customerToUpdate);
    }

    @Override
    public void deleteCustomer(Long id) throws Exception {
        Customer customerToDelete=customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        customerRepository.delete(customerToDelete);

    }

    @Override
    public Customer getCustomerById(Long id) throws Exception {
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Override
    public List<Customer> getAllCustomers() throws Exception {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> searchCustomers(String keyword) throws Exception {
        return customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword); 
    }
}
