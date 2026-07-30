package com.molla.repository;

import com.molla.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);
    
    Optional<Product> findBySku(String sku);
    
    // findAll() is already inherited from JpaRepository

    @Query("""
        SELECT p FROM Product p
        WHERE p.store.id = :storeId
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
        )
    """)
    List<Product> searchByKeyword(
            @Param("storeId") Long storeId,
            @Param("query") String query
    );
}