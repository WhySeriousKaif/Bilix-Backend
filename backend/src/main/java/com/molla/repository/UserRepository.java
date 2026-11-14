package com.molla.repository;

import com.molla.domain.UserRole;
import com.molla.model.Store;
import com.molla.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    User findByEmail(String email);
    List<User> findByStore(Store store);
    List<User> findByStoreAndRoleIn(Store store, List<UserRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.branch.id = :branchId")
    List<User> findByBranchId(@Param("branchId") Long branchId);

    default Page<User> findAllFiltered(String search, UserRole role, Pageable pageable) {
        // Omit absent filters so PostgreSQL never has to infer a null parameter's type.
        Specification<User> filters = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (search != null) {
                var pattern = cb.lower(cb.literal("%" + search + "%"));
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (role != null) {
                predicate = cb.and(predicate, cb.equal(root.get("role"), role));
            }
            return predicate;
        };
        return findAll(filters, pageable);
    }
}
