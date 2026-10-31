package com.molla.service;

import com.molla.model.Customer;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CustomerService {

    Customer createCustomer(Customer customer);
    Customer updateCustomer(Customer customer,Long id) throws Exception;
    void deleteCustomer(Long id) throws Exception;
    Customer getCustomerById(Long id) throws Exception; 
    List<Customer> getAllCustomers() throws Exception;
    List<Customer>  searchCustomers(String keyword) throws Exception;
    Page<Customer> getCustomers(String keyword, Pageable pageable);

    
}
