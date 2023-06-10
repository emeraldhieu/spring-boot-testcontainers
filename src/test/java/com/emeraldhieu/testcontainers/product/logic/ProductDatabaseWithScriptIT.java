package com.emeraldhieu.testcontainers.product.logic;

import com.emeraldhieu.testcontainers.product.ProductApp;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    classes = ProductApp.class,
    properties = {
        // Disable Kafka for this test only
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    }
)
@Testcontainers
public class ProductDatabaseWithScriptIT {

    private static Logger logger = LoggerFactory.getLogger(ProductDatabaseWithScriptIT.class);
    private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);

    /**
     * NOTE: Static instance means there's only one database instance used for the whole test class.
     * All tests methods can trample on each other's data.
     */
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3-alpine"))
            .withUsername("postgres")
            .withPassword("postgres")
            .withLogConsumer(logConsumer)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withClasspathResourceMapping("postgres-scripts/createMultipleDatabases.sh",
                "/docker-entrypoint-initdb.d/createMultipleDatabases.sh", BindMode.READ_ONLY)
            .withEnv("POSTGRES_MULTIPLE_DATABASES", "product"); // Use the same name for database, username, and password.

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        /**
         * Since docker container generates a random port,
         * the JDBC URL on the fly has to be set on the fly.
         */
        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductService productService;

    /**
     * Override with an empty bean to avoid Kafka things.
     */
    @MockBean
    private ProductEventListener productEventListener;

    @Test
    @Transactional
    public void givenProduct_whenCreate_thenReturnProduct() {
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
    }
}
