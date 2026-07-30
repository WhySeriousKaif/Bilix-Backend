package com.molla.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🚦 Rate Limiting Filter - Prevents API abuse by limiting requests per IP
 * 
 * 👉 Purpose: Protect API from brute-force attacks and excessive requests
 * 🔹 Strategy: In-memory sliding window (1-minute buckets) per IP address
 * 
 * 📌 How it works:
 * - Track request count per IP + endpoint bucket (LOGIN vs API)
 * - Reset counter every minute (sliding window)
 * - Block requests exceeding limit (return 429 Too Many Requests)
 * 
 * ⚠️ Note: This is NOT production-grade (in-memory, lost on restart)
 *    For production: Use Redis or distributed rate limiting
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /**
     * 📊 Counter Record - Stores request count for a time window
     * 
     * 👉 Purpose: Track how many requests in current 1-minute window
     * 🔹 Fields:
     * - windowStartEpochSecond: Start of current minute (for window tracking)
     * - count: Number of requests in this window
     */
    private record Counter(long windowStartEpochSecond, int count) {}

    // 📝 In-memory storage: IP + bucket → Counter
    // 🔹 Why ConcurrentHashMap? Thread-safe for concurrent requests
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    // 🚦 Rate Limits (requests per minute)
    private static final int LOGIN_LIMIT_PER_MINUTE = 10;  // 🔐 Stricter for login (prevent brute-force)
    private static final int DEFAULT_LIMIT_PER_MINUTE = 120;  // 📡 Higher limit for general API calls

    /**
     * 🔍 Core Filter Logic - Runs on every HTTP request
     * 
     * 👉 Purpose: Check if request exceeds rate limit, block if yes
     * 🔹 Flow: Extract IP → Determine bucket → Check counter → Increment/Reset → Allow/Block
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";

        // ⏭️ Skip rate limiting for Swagger/docs (not external API calls)
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🪣 Determine bucket: LOGIN (stricter) or API (more lenient)
        String bucket = path.startsWith("/auth/login") ? "LOGIN" : "API";
        int limit = bucket.equals("LOGIN") ? LOGIN_LIMIT_PER_MINUTE : DEFAULT_LIMIT_PER_MINUTE;

        // 🔑 Create unique key: IP address + bucket type
        String key = clientIp + ":" + bucket;
        
        // ⏰ Calculate current 1-minute window (sliding window)
        long nowSec = Instant.now().getEpochSecond();
        long windowStart = nowSec / 60;  // Round down to minute boundary

        // 📊 Update counter: Increment if same window, reset if new window
        Counter updated = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStartEpochSecond != windowStart) {
                // 🆕 New window or first request → Start with count = 1
                return new Counter(windowStart, 1);
            }
            // ➕ Same window → Increment count
            return new Counter(existing.windowStartEpochSecond, existing.count + 1);
        });

        // 🚫 Check if limit exceeded
        if (updated.count > limit) {
            // ❌ Block request → Return 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;  // ⛔ Stop filter chain (request doesn't reach controller)
        }

        // ✅ Within limit → Continue to next filter/controller
        filterChain.doFilter(request, response);
    }
}
