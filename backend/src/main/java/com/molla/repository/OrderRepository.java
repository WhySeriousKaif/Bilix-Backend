package com.molla.repository;

import com.molla.model.Order;
import com.molla.model.User;
import com.molla.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    List<Order> findByCustomerId(Long customerId);
    List<Order> findByBranchId(Long branchId);
    List<Order> findByCashierId(Long cashierId);
    List<Order> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
    List<Order> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime startDate, LocalDateTime endDate);

    List<Order> findTop5ByBranchIdOrderByCreatedAtDesc(Long branchId);

    // Pagination + sorting for orders list by branch
    Page<Order> findByBranchId(Long branchId, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.customer customer
            LEFT JOIN o.cashier cashier
            LEFT JOIN o.branch branch
            LEFT JOIN branch.store store
            WHERE (:search IS NULL
                OR LOWER(customer.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(cashier.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(cashier.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(store.brand) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR o.status = :status)
              AND (:storeId IS NULL OR store.id = :storeId)
            """)
    Page<Order> findSystemSalesFiltered(
            @Param("search") String search,
            @Param("status") OrderStatus status,
            @Param("storeId") Long storeId,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double sumSystemSales();

    /**
     * Every order belonging to a store inside a half-open [start, end) window, newest first,
     * with branch and line items already fetched. Used by the store dashboard so one request
     * can derive all of its aggregates without an N+1 fan-out per branch.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.branch b
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product
            WHERE b.store.id = :storeId
              AND o.createdAt >= :start
              AND o.createdAt < :end
            ORDER BY o.createdAt DESC
            """)
    List<Order> findStoreOrdersInRange(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o.createdAt, o.totalAmount FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
    List<Object[]> findSalesRowsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.paymentType, COALESCE(SUM(o.totalAmount), 0) FROM Order o GROUP BY o.paymentType")
    List<Object[]> sumSalesByPaymentType();

    @Query("SELECT o.branch.store.id, COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o GROUP BY o.branch.store.id")
    List<Object[]> sumSalesByStore();

    @Query("SELECT o.branch.id, COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o GROUP BY o.branch.id")
    List<Object[]> sumSalesByBranch();
}
