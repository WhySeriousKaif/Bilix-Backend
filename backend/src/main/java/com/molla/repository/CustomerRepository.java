package com.molla.repository;

import com.molla.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
    
}
