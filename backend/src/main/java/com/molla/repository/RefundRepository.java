package com.molla.repository;

import com.molla.model.Refund;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    List<Refund> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Refund> findByCashierId(Long cashierId);
    
    List<Refund> findByShiftReportId(Long shiftReportId);
    
    List<Refund> findByBranchId(Long branchId);

    List<Refund> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r")
    Double sumSystemRefundAmount();
}
