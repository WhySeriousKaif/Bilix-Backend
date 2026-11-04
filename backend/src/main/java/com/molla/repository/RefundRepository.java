package com.molla.repository;

import com.molla.model.Refund;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.molla.domain.PaymentType;

import java.time.LocalDateTime;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    List<Refund> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Refund> findByCashierId(Long cashierId);
    
    List<Refund> findByShiftReportId(Long shiftReportId);
    
    List<Refund> findByBranchId(Long branchId);
    Page<Refund> findByBranchId(Long branchId, Pageable pageable);

    @Query("""
            SELECT r FROM Refund r WHERE r.branch.id = :branchId
              AND (:from IS NULL OR r.createdAt >= :from)
              AND (:payment IS NULL OR r.paymentType = :payment)
              AND (:search IS NULL OR r.id = :refundId OR r.order.id = :orderId
                   OR LOWER(r.reason) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Refund> findBranchRefundsFiltered(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("payment") PaymentType payment,
            @Param("search") String search,
            @Param("refundId") Long refundId,
            @Param("orderId") Long orderId,
            Pageable pageable);

    List<Refund> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r")
    Double sumSystemRefundAmount();
}
