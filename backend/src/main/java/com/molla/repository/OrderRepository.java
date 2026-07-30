package com.molla.repository;

import com.molla.model.Order;
import com.molla.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByBranchId(Long branchId);
    List<Order> findByCashierId(Long cashierId);
    List<Order> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
    List<Order> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime startDate, LocalDateTime endDate);

    List<Order> findTop5ByBranchIdOrderByCreatedAtDesc(Long branchId);

    // Pagination + sorting for orders list by branch
    Page<Order> findByBranchId(Long branchId, Pageable pageable);
}
