package com.molla.repository;

import com.molla.domain.UserRole;
import com.molla.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository repository;

    @Test
    void optionalFiltersPreserveSearchRolesAndPagination() {
        repository.deleteAll();
        saveUser("Alice", "alice@example.com", UserRole.ROLE_ADMIN);
        saveUser("Bob", "bob@example.com", UserRole.ROLE_CASHIER);
        var page = PageRequest.of(0, 1, Sort.by("fullName"));

        var all = repository.findAllFiltered(null, null, page);
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getContent()).extracting(User::getFullName).containsExactly("Alice");
        assertThat(repository.findAllFiltered(null, UserRole.ROLE_CASHIER, page).getContent())
                .extracting(User::getFullName).containsExactly("Bob");
        assertThat(repository.findAllFiltered("ALICE", null, page).getContent())
                .extracting(User::getFullName).containsExactly("Alice");
        assertThat(repository.findAllFiltered("EXAMPLE.COM", UserRole.ROLE_CASHIER, page).getContent())
                .extracting(User::getFullName).containsExactly("Bob");
        assertThat(repository.findAllFiltered("Alice", UserRole.ROLE_CASHIER, page).getTotalElements())
                .isZero();
    }

    private void saveUser(String name, String email, UserRole role) {
        var user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword("unused-test-hash");
        user.setRole(role);
        repository.saveAndFlush(user);
    }
}
