package com.emeraldhieu.testcontainers.product.logic.containers;

import com.emeraldhieu.testcontainers.product.ProductApp;
import com.emeraldhieu.testcontainers.product.ProductMessage;
import com.emeraldhieu.testcontainers.product.logic.ProductController;
import com.emeraldhieu.testcontainers.product.logic.ProductRequest;
import com.emeraldhieu.testcontainers.product.logic.ProductResponse;
import com.emeraldhieu.testcontainers.product.logic.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * A test that tests when event listener fails to send message, the database rollbacks the inserted record.
 */
@SpringBootTest(
    classes = ProductApp.class,
    properties = {
        // Disable Kafka auto-configuration
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    }
)
@Testcontainers
@Slf4j
public class PostgresAndFailedKafkaTemplateIT {

    private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

    /**
     * NOTE: Static instance means there's only one database instance used for the whole test class.
     * All tests methods can trample on each other's data.
     */
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.3-alpine"))
            .withLogConsumer(logConsumer);

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

    @MockBean
    private KafkaTemplate<String, ProductMessage> kafkaTemplate;

    @Autowired
    private ProductController productController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
            .build();
    }

    @Test
    void givenKafkaTemplateSendFails_whenCreateProduct_thenShouldNotSaveProduct() throws Exception {
        // GIVEN
        String name = "awesomePizza";
        double price = 42;
        ProductRequest productRequest = ProductRequest.builder()
            .name(name)
            .price(price)
            .build();

        String errorMessage = "Failed to send message";
        doThrow(new IllegalArgumentException(errorMessage))
            .when(kafkaTemplate).send(any(), any());

        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/products")
                .content(objectMapper.writeValueAsString(productRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            );
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e.getCause(), errorMessage);
        }

        // THEN
        List<ProductResponse> products = productService.list(0, 10).getContent();
        List<String> productNames = products.stream()
            .map(ProductResponse::getName)
            .collect(Collectors.toList());
        assertFalse(productNames.contains(name));
    }
}
