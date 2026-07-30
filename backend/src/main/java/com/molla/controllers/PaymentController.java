package com.molla.controllers;

import com.molla.util.RazorPay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired
    private RazorPay razorPay;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam Double amount, @RequestParam String currency) {
        try {
            Map<String, Object> order = razorPay.createOrder(amount, currency, "receipt#" + System.currentTimeMillis());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
