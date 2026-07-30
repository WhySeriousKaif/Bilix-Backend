package com.molla.service;

import com.molla.model.User;

import java.util.List;

public interface UserService {

    User getUserFromJwt(String jwt);

    User getCurrentUser();

    User getUserByEmail(String email);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(User user);
}
