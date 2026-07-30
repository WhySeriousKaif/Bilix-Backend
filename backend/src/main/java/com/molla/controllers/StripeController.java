package com.molla.controllers;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class StripeController {

    @Value("${stripe.public.key}")
    private String publicKey;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestParam(required = false, defaultValue = "1000") Long amount,
            @RequestParam(required = false, defaultValue = "usd") String currency,
            @RequestParam(required = false, defaultValue = "Test Product") String productName) {
        try {
            // Create session parameters
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:5001/api/payment/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:5001/api/payment/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.toLowerCase())
                                                    .setUnitAmount(amount) // Amount in cents (e.g., 1000 = $10.00)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(productName)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .build();

            // Create checkout session
            Session session = Session.create(params);

            // Return session ID and public key
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("publicKey", publicKey);

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating checkout session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/success")
    public String success(@RequestParam(required = false) String session_id) {
        return "<html><body><h1>Payment Successful!</h1><p>Session ID: " + 
               (session_id != null ? session_id : "N/A") + 
               "</p><a href='/'>Return to Home</a></body></html>";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "<html><body><h1>Payment Cancelled</h1><p>You cancelled the payment.</p><a href='/'>Return to Home</a></body></html>";
    }
}
