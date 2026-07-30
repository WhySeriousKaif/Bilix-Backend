package com.molla.repository;

import com.molla.domain.UserRole;
import com.molla.model.Store;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    List<User> findByStore(Store store);
    List<User> findByStoreAndRoleIn(Store store, List<UserRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.branch.id = :branchId")
    List<User> findByBranchId(@Param("branchId") Long branchId);
}
