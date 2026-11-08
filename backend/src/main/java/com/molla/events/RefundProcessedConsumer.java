package com.molla.events;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("kafka")
public class RefundProcessedConsumer {

    @KafkaListener(
            topics = "refund-events",
            groupId = "bilix-refund-audit"
    )
    public void handle(RefundProcessedEvent event){
        System.out.println(
                "Refund event received: refundId="
                        + event.refundId()
                        + ", orderId="
                        + event.orderId()
                        + ", amount="
                        + event.amount()
        );
    }
}
