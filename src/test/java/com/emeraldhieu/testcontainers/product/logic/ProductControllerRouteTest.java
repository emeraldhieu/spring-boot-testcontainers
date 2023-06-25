package com.emeraldhieu.testcontainers.product.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A simple unit test that tests controller routing.
 * It runs fast because no application context is loaded.
 * See https://thepracticaldeveloper.com/guide-spring-boot-controller-tests/#strategy-1-spring-mockmvc-example-in-standalone-modemode
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerRouteTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private String id;
    private String name;
    private double price;
    private ProductResponse productResponse;

    @BeforeEach
    public void setUp() {
        /**
         * I manually create mockMvc because I want to set only necessary components.
         * If @WebMvcTest was used, it would load even @ControllerAdvice that is redundant.
         * See https://thepracticaldeveloper.com/guide-spring-boot-controller-tests/#strategy-1-spring-mockmvc-example-in-standalone-modemode
         * Keep it simple and fast.
         */
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
            .build();

        id = "pizza";
        name = "Pizza";
        price = 42;
        productResponse = ProductResponse.builder()
            .id(id)
            .name(name)
            .price(price)
            .build();
    }

    @Test
    void givenProductService_whenListProducts_thenReturnProducts() throws Exception {
        // GIVEN
        int offset = 0;
        int limit = 10;
        Page<ProductResponse> productResponses = new PageImpl<>(List.of(
            productResponse
        ));
        when(productService.list(offset, limit))
            .thenReturn(productResponses);

        // WHEN and THEN
        mockMvc.perform(MockMvcRequestBuilders.get("/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", equalTo(id)))
            .andExpect(jsonPath("$[0].name", equalTo(name)))
            .andExpect(jsonPath("$[0].price", equalTo(price)));
    }

    @Test
    void givenProductService_whenCreateProduct_thenReturnAnProduct() throws Exception {
        // GIVEN
        ProductRequest productRequest = ProductRequest.builder()
            .name(name)
            .price(price)
            .build();
        when(productService.create(productRequest)).thenReturn(productResponse);

        // WHEN and THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/products")
                .content(objectMapper.writeValueAsString(productRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", equalTo(id)))
            .andExpect(jsonPath("$.name", equalTo(name)))
            .andExpect(jsonPath("$.price", equalTo(price)));
    }

    @Test
    void givenProductService_whenGetProduct_thenReturnAnProduct() throws Exception {
        // GIVEN
        when(productService.get(id)).thenReturn(productResponse);

        // WHEN and THEN
        mockMvc.perform(MockMvcRequestBuilders.get("/products/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(id)))
            .andExpect(jsonPath("$.name", equalTo(name)))
            .andExpect(jsonPath("$.price", equalTo(price)));
    }

    @Test
    void givenProductService_whenUpdateProduct_thenReturnAnProduct() throws Exception {
        // GIVEN
        String nameToUpdate = name + "updated";
        double priceToUpdate = price + 42;
        ProductRequest productRequest = ProductRequest.builder()
            .name(name)
            .price(price)
            .build();
        ProductResponse updatedProductResponse = productResponse.toBuilder()
            .name(nameToUpdate)
            .price(priceToUpdate)
            .build();
        when(productService.update(id, productRequest)).thenReturn(updatedProductResponse);

        // WHEN and THEN
        mockMvc.perform(MockMvcRequestBuilders.patch("/products/{id}", id)
                .content(objectMapper.writeValueAsString(productRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(id)))
            .andExpect(jsonPath("$.name", equalTo(nameToUpdate)))
            .andExpect(jsonPath("$.price", equalTo(priceToUpdate)));
    }

    @Test
    void givenProductService_whenDeleteProduct_thenReturnNoContent() throws Exception {
        // WHEN and THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/products/{id}", id))
            .andExpect(status().isNoContent());
        verify(productService, times(1)).delete(id);
    }
}