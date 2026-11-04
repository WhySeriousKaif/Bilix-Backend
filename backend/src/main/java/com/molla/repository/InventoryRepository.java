package com.molla.repository;

import com.molla.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId and i.branch.id = :branchId")
    Optional<Inventory> findByProductIdAndBranchIdForUpdate(@Param("productId") Long productId,
                                                              @Param("branchId") Long branchId);

    List<Inventory> findByBranchId(Long branchId);
    Page<Inventory> findByBranchId(Long branchId, Pageable pageable);
}
