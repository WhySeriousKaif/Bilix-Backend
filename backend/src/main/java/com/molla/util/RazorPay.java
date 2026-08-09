package com.molla.util;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RazorPay {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    public Map<String, Object> createOrder(Double amount, String currency, String receipt) throws RazorpayException {
        try {
            System.out.println("DEBUG: Creating Razorpay order. Amount: " + amount + ", Currency: " + currency);
            if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
                System.out.println("ERROR: Razorpay API Key or Secret is missing!");
                throw new RazorpayException("Razorpay API Key or Secret is not configured.");
            }

            // Logic to create order using RazorPay API
            RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", Math.round(amount * 100)); // amount in the smallest currency unit (paise)
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            Order order = razorpayClient.orders.create(orderRequest);

            // Convert Order to Map for response
            Map<String, Object> orderResponse = new HashMap<>();

            // Use .toString() which returns the string representation, avoiding internal
            // casting issues
            // Retrieve values as Objects first
            Object idVal = order.get("id");
            Object amountVal = order.get("amount");
            Object currencyVal = order.get("currency");
            Object receiptVal = order.get("receipt");
            Object statusVal = order.get("status");
            Object createdAtVal = order.get("created_at");

            orderResponse.put("id", idVal != null ? String.valueOf(idVal) : "");

            // Amount is usually an Integer, but handle it safely
            if (amountVal instanceof Number) {
                orderResponse.put("amount", ((Number) amountVal).intValue());
            } else if (amountVal != null) {
                try {
                    orderResponse.put("amount", Integer.parseInt(String.valueOf(amountVal)));
                } catch (NumberFormatException e) {
                    orderResponse.put("amount", 0);
                }
            } else {
                orderResponse.put("amount", 0);
            }

            orderResponse.put("currency", currencyVal != null ? String.valueOf(currencyVal) : "INR");
            orderResponse.put("receipt", receiptVal != null ? String.valueOf(receiptVal) : "");
            orderResponse.put("status", statusVal != null ? String.valueOf(statusVal) : "");
            orderResponse.put("created_at", createdAtVal != null ? String.valueOf(createdAtVal) : "");

            // Include the Public Key so frontend uses the matching key
            orderResponse.put("key", apiKey);

            System.out.println("DEBUG: Razorpay order created successfully: " + orderResponse.get("id"));

            return orderResponse;
        } catch (RazorpayException e) {
            System.out.println("ERROR: RazorpayException in createOrder: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected exception in createOrder: " + e.getMessage());
            e.printStackTrace();
            throw new RazorpayException("Internal Error: " + e.getMessage());
        }
    }

    /**
     * Recomputes the HMAC-SHA256 signature Razorpay sends after a successful checkout
     * and compares it against what the client reported. Without this, a client could
     * fabricate a fake payment_id/signature pair and get an order marked as paid
     * without ever actually paying — this is what actually proves the payment happened.
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, apiSecret);
        } catch (Exception e) {
            System.out.println("ERROR: Signature verification failed: " + e.getMessage());
            return false;
        }
    }

}
