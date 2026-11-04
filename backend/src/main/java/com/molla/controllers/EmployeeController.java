package com.molla.controllers;

import com.molla.domain.UserRole;
import com.molla.exceptions.UserException;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.ApiResponse;
import com.molla.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/store/{storeId}")
    public ResponseEntity<UserDto> createStoreEmployee(
            @PathVariable("storeId") Long storeId,
            @RequestBody UserDto employee) throws UserException {
        UserDto createdEmployee = employeeService.createStoreEmployee(employee, storeId);
        return ResponseEntity.status(HttpStatus.SC_CREATED).body(createdEmployee);
    }

    @PostMapping("/branch/{branchId}")
    public ResponseEntity<UserDto> createBranchEmployee(
            @PathVariable("branchId") Long branchId,
            @RequestBody UserDto employeeDetails) throws UserException {
        UserDto createdEmployee = employeeService.createBranchEmployee(employeeDetails, branchId);
        return ResponseEntity.ok(createdEmployee);
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<UserDto> updateEmployee(
            @PathVariable("employeeId") Long employeeId,
            @RequestBody UserDto employeeDetails) throws UserException {
        UserDto updatedEmployee = employeeService.updateEmployee(employeeDetails, employeeId);
        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponse> deleteEmployee(
            @PathVariable("employeeId") Long employeeId) throws UserException {
        employeeService.deleteEmployee(employeeId);
        ApiResponse apiResponse = new ApiResponse("Employee deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<UserDto>> findStoreEmployees(
            @PathVariable("storeId") Long storeId,
            @RequestParam(required = false) UserRole role) throws UserException {
        List<UserDto> employees = employeeService.findStoreEmployees(storeId, role);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<UserDto>> findBranchEmployees(
            @PathVariable("branchId") Long branchId,
            @RequestParam(required = false) UserRole role) throws UserException {
        List<UserDto> employees = employeeService.findBranchEmployees(branchId, role);
        return ResponseEntity.ok(employees);
    }
}

