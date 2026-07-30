package com.molla.repository;

import com.molla.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

    List<Inventory> findByBranchId(Long branchId);
}
