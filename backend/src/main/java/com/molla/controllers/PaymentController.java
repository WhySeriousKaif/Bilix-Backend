package com.molla.controllers;

import com.molla.util.RazorPay;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // Razorpay's own minimum: an order below 1 rupee (100 paise) is rejected by their API anyway,
    // but failing fast here gives a clear message instead of a confusing Razorpay error round-trip.
    private static final double MINIMUM_AMOUNT_RUPEES = 1.0;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam Double amount, @RequestParam String currency) {
        if (amount == null || amount < MINIMUM_AMOUNT_RUPEES) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Amount must be at least ₹" + MINIMUM_AMOUNT_RUPEES));
        }
        try {
            Map<String, Object> order = razorPay.createOrder(amount, currency, "receipt#" + System.currentTimeMillis());
            return ResponseEntity.ok(order);
        } catch (RazorpayException e) {
            // Razorpay auth failures (bad key/secret) surface here as a RazorpayException too;
            // distinguish them so a misconfigured key doesn't look like a generic server crash.
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (message.toLowerCase().contains("authentication") || message.toLowerCase().contains("key_id")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Razorpay authentication failed: " + message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating order: " + message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        if (orderId == null || paymentId == null || signature == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("verified", false, "error", "razorpay_order_id, razorpay_payment_id and razorpay_signature are all required"));
        }

        boolean verified = razorPay.verifyPaymentSignature(orderId, paymentId, signature);
        if (!verified) {
            // Do NOT mark the order as paid — the caller must treat this as a failed payment.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("verified", false, "error", "Payment signature verification failed"));
        }
        return ResponseEntity.ok(Map.of("verified", true));
    }
}
