package com.molla.repository;

import com.molla.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);
    List<Product> findByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);
    Page<Product> findByStoreId(Long storeId, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN p.category category
        WHERE p.store.id = :storeId
        AND (:search IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(category.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> findByStoreIdFiltered(
            @Param("storeId") Long storeId,
            @Param("search") String search,
            Pageable pageable);
    
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
