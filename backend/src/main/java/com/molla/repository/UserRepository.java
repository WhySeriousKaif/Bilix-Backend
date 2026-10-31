package com.molla.repository;

import com.molla.domain.UserRole;
import com.molla.model.Store;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    List<User> findByStore(Store store);
    List<User> findByStoreAndRoleIn(Store store, List<UserRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.branch.id = :branchId")
    List<User> findByBranchId(@Param("branchId") Long branchId);

    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> findAllFiltered(
            @Param("search") String search,
            @Param("role") UserRole role,
            Pageable pageable);
}
