package com.molla.service;

import com.molla.domain.UserRole;
import com.molla.exceptions.UserException;
import com.molla.payload.dto.UserDto;

import java.util.List;

public interface EmployeeService {
    UserDto createStoreEmployee(UserDto employee,Long storeId) throws UserException;
    UserDto createBranchEmployee(UserDto employeeDetails, Long branchId) throws UserException;
    UserDto updateEmployee(UserDto employeeDetails, Long employeeId) throws UserException;

    void deleteEmployee(Long employeeId) throws UserException;
    List<UserDto> findStoreEmployees(Long storeId,UserRole role) throws UserException;
    List<UserDto> findBranchEmployees(Long branchId,UserRole role) throws UserException;
}
