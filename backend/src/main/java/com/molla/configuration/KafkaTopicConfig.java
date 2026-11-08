package com.molla.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("kafka")
public class KafkaTopicConfig {
    @Bean
    public NewTopic refundEventsTopic(){
        return  new NewTopic(
                "refund-events",
                1,
                (short)1
        );

    }
}
