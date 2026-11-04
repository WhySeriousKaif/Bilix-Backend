package com.molla.repository;

import com.molla.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

    @Query("select distinct c from Customer c join Order o on o.customer.id = c.id where o.branch.id = :branchId")
    Page<Customer> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query("select distinct c from Customer c join Order o on o.customer.id = c.id where o.branch.id = :branchId and (lower(c.name) like lower(concat('%', :term, '%')) or lower(c.email) like lower(concat('%', :term, '%')) or c.phone like concat('%', :term, '%'))")
    Page<Customer> findByBranchIdAndSearch(@Param("branchId") Long branchId, @Param("term") String term, Pageable pageable);
    
}
