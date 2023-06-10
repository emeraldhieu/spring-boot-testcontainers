package com.emeraldhieu.testcontainers.product.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    /**
     * Create a topic if not existed.
     */
    @Bean
    public NewTopic createTopic() {
        return new NewTopic(kafkaProperties.getTopic(),
            kafkaProperties.getPartitions(),
            (short) kafkaProperties.getReplicationFactor());
    }
}