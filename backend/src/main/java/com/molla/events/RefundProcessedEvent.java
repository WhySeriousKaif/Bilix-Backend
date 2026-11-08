package com.molla.events;

import java.time.LocalDateTime;

public record RefundProcessedEvent(
        Long refundId,
        Long orderId,
        Long branchId,
        Long cachierId,
        double amount,
        String paymentId,
        LocalDateTime processedAt

) {
}
