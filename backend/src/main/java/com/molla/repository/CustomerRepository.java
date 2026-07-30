package com.molla.repository;

import com.molla.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    
}
