package com.molla.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.molla.model.Customer;
import com.molla.service.CustomerService;
import com.molla.payload.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable("id") Long id,@RequestBody Customer customer) throws Exception {
        return ResponseEntity.ok(customerService.updateCustomer(customer, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable("id") Long id) throws Exception {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(new ApiResponse("Customer deleted successfully"));
    }


    @GetMapping 
    public ResponseEntity<List<Customer>> getAllCustomers() throws Exception {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable("id") Long id) throws Exception {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "q", required = false) String q) throws Exception {
        // Accept both 'keyword' and 'q' parameters
        String searchTerm = keyword != null ? keyword : (q != null ? q : "");
        if (searchTerm.isEmpty()) {
            throw new IllegalArgumentException("Either 'keyword' or 'q' parameter is required");
        }
        return ResponseEntity.ok(customerService.searchCustomers(searchTerm));
    }
}
