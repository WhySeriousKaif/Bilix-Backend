package com.molla.service;

import com.molla.model.User;

import java.util.List;
import com.molla.domain.UserRole;
import org.springframework.data.domain.Page;

public interface UserService {

    User getUserFromJwt(String jwt);

    User getCurrentUser();

    User getUserByEmail(String email);

    User getUserById(Long id);

    List<User> getAllUsers();

    Page<User> getUsersPaged(int page, int size, String sortBy, String direction, String search, UserRole role);

    User updateUser(User user);
}
