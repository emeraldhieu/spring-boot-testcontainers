package com.emeraldhieu.testcontainers.product.logic;

import com.emeraldhieu.testcontainers.product.ProductApp;
import com.emeraldhieu.testcontainers.product.ProductMessage;
import com.emeraldhieu.testcontainers.product.config.KafkaTestConfiguration;
import com.emeraldhieu.testcontainers.product.config.KafkaTestProperties;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    classes = ProductApp.class
)
@Testcontainers
@Import({
    KafkaTestConfiguration.class,
})
public class ProductDatabaseAndKafkaIT {

    private static Logger logger = LoggerFactory.getLogger(ProductDatabaseAndKafkaIT.class);
    private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);

    /**
     * NOTE: Static instance means there's only one database instance used for the whole test class.
     * All tests methods can trample on each other's data.
     */
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3-alpine"))
            .withLogConsumer(logConsumer);

    static Network network = Network.newNetwork();

    // Cluster ID is created by "kafka-storage random-uuid"
    private static String clusterId = "qYoMEZXcS_SKP2PzAl8-WA";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
        .withNetwork(network)
        .withKraft()
        .withClusterId(clusterId)
        .withLogConsumer(logConsumer);

    @Container
    static GenericContainer schemaRegistry =
        new GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.4.0"))
            .withNetwork(network)
            .withExposedPorts(8081) // Exposed port is used to get a mapped port. Otherwise, the error "Container doesn't expose any ports" occurs.
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry") // To be resolved by Docker
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081") // Seems optional
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092") // A list of Kafka brokers to connect to
            .dependsOn(kafka)
            .withLogConsumer(logConsumer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        /**
         * Configure Spring's datasource on the fly.
         * This seems like the only working way.
         * I've tried using "application.yml" but it doesn't work.
         */
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url",
            () -> "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort());
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private KafkaTestProperties kafkaTestProperties;

    @Autowired
    private ConsumerFactory consumerFactory;

    @BeforeEach
    public void setUp() {
        /**
         * Manually set consumerFactory for KafkaTemplate.
         * Not sure why {@link KafkaAutoConfiguration#kafkaTemplate(ProducerFactory, ProducerListener, ObjectProvider)}
         * doesn't set {@link ConsumerFactory}.
         */
        kafkaTemplate.setConsumerFactory(consumerFactory);
    }

    @Test
    @Transactional
    public void givenProductRequest_whenCreate_thenReturnProductAndSendKafkaMessage() {
        // GIVEN
        String name = "pizza";
        double price = 42;
        ProductRequest productRequest = ProductRequest.builder()
            .name(name)
            .price(price)
            .build();

        // WHEN
        ProductResponse productResponse = productService.create(productRequest);

        // THEN
        assertEquals(name, productResponse.getName());
        assertEquals(price, productResponse.getPrice());

        ProductMessage expectedMessage = ProductMessage.newBuilder()
            .setId(productResponse.getId())
            .setName(productResponse.getName())
            .setPrice(productResponse.getPrice())
            .build();
        ConsumerRecord<String, ProductMessage> consumerRecord = kafkaTemplate.receive(kafkaTestProperties.getTopic(), 0, 0);
        assertEquals(expectedMessage, consumerRecord.value());
    }
}
