package com.molla.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("kafka")
public class RefundProcessedKafkaPublisher {

    private  final KafkaTemplate<String,RefundProcessedEvent>kafkaTemplate;

    public RefundProcessedKafkaPublisher(
            KafkaTemplate<String, RefundProcessedEvent> kafkaTemplate
    ){
        this.kafkaTemplate=kafkaTemplate;

    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publish(RefundProcessedEvent event){
        kafkaTemplate.send(
                "refund-event",
                String.valueOf(event.refundId()),
                event
        );
    }



}
