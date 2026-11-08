package com.molla.configuration;

import com.molla.events.RefundProcessedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("kafka")
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, RefundProcessedEvent>refundProducerFactory(Environment environment){
        Map<String,Object> properties=new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty(
                        "spring.kafka.bootstrap-servers",
                        "localhost:9092"
                )
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        return new DefaultKafkaProducerFactory<>(properties);



    }
    @Bean
    public KafkaTemplate<String,RefundProcessedEvent> refundKafkaTemplate(
            ProducerFactory<String, RefundProcessedEvent> factory
    ){
            return new KafkaTemplate<>(factory);
    }
}
