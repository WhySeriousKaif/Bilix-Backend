package com.molla.repository;

import com.molla.model.Refund;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    List<Refund> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Refund> findByCashierId(Long cashierId);
    
    List<Refund> findByShiftReportId(Long shiftReportId);
    
    List<Refund> findByBranchId(Long branchId);
}

