package com.emeraldhieu.testcontainers.product.config;

import com.emeraldhieu.testcontainers.product.ProductMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;

import java.util.Map;

@ConditionalOnProperty(value = "application.kafka.enabled", havingValue = "true") // used to disable Kafka in tests
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    /**
     * Customize the autoconfigured {@link DefaultKafkaProducerFactory} using Java code.
     */
    @Bean
    public DefaultKafkaProducerFactoryCustomizer defaultKafkaProducerFactoryCustomizer() {
        return producerFactory -> {
            Map<String, Object> additionalConfigs = Map.of(
                /**
                 * Increase the "upper bound on the time to report success or failure after a call to send() returns".
                 * See https://kafka.apache.org/documentation/#producerconfigs_delivery.timeout.ms
                 */
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 180000
            );
            producerFactory.updateConfigs(additionalConfigs);
        };
    }

    /**
     * Try listening to the producer to see what's going on.
     */
    @Bean
    public KafkaTemplate<String, ProductMessage> kafkaTemplate(ProducerFactory producerFactory) {
        KafkaTemplate<String, ProductMessage> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, ProductMessage> producerRecord, RecordMetadata recordMetadata) {
                log.info("Sent a record");
            }

            @Override
            public void onError(ProducerRecord<String, ProductMessage> producerRecord, RecordMetadata recordMetadata, Exception exception) {
                log.info("Fail to send a record");
            }
        });
        return kafkaTemplate;
    }

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