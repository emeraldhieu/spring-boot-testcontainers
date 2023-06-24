package com.emeraldhieu.testcontainers.product.logic;

import com.emeraldhieu.testcontainers.product.logic.mapping.ProductRequestMapper;
import com.emeraldhieu.testcontainers.product.logic.mapping.ProductResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProductServiceTest {

    private ProductRepository productRepository;
    private ProductRequestMapper productRequestMapper;
    private ProductResponseMapper productResponseMapper;
    private ApplicationEventPublisher applicationEventPublisher;
    private DefaultProductService defaultProductService;

    @BeforeEach
    public void setUp() {
        productRepository = mock(ProductRepository.class);
        productRequestMapper = mock(ProductRequestMapper.class);
        productResponseMapper = mock(ProductResponseMapper.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        defaultProductService = new DefaultProductService(productRepository, productRequestMapper,
            productResponseMapper, applicationEventPublisher);
    }

    @Test
    void givenProductRequest_whenCreate_thenPublishEventAndReturnProductResponse() {
        // GIVEN
        ProductRequest productRequest = ProductRequest.builder()
            .build();
        Product productToSave = Product.builder()
            .build();
        String externalId = "amazingExternalId";
        String name = "pizza";
        double price = 42;
        Product savedProduct = Product.builder()
            .price(price)
            .name(name)
            .externalId(externalId)
            .build();

        when(productRequestMapper.toEntity(productRequest)).thenReturn(productToSave);
        when(productRepository.save(productToSave)).thenReturn(savedProduct);

        ProductResponse expectedProductResponse = ProductResponse.builder()
            .id(externalId)
            .build();
        when(productResponseMapper.toDto(savedProduct)).thenReturn(expectedProductResponse);

        // WHEN
        ProductResponse productResponse = defaultProductService.create(productRequest);

        // THEN
        ProductCreatedEvent event = ProductCreatedEvent.builder()
            .id(externalId)
            .name(name)
            .price(price)
            .build();
        verify(applicationEventPublisher, times(1)).publishEvent(event);
        assertEquals(expectedProductResponse, productResponse);
    }

    @Test
    void givenProductIdAndRequest_whenUpdate_thenReturnProductResponse() {
        // GIVEN
        String externalId = "amazingExternalId";
        String productName = "product42";
        String productNameToUpdate = "updatedProduct42";
        ProductRequest productRequest = ProductRequest.builder()
            .name(productNameToUpdate)
            .build();
        Product retrievedProduct = Product.builder()
            .externalId(externalId)
            .name(productName)
            .build();
        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(retrievedProduct));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            Product productArg = (Product) args[0];
            ProductRequest productRequestArg = (ProductRequest) args[1];
            productArg.setName(productRequestArg.getName());
            return null;
        }).when(productRequestMapper).partialUpdate(retrievedProduct, productRequest);

        /**
         * TODO Find a way to initialize #productToUpdate based on "productArg" above
         */
        Product productToUpdate = Product.builder()
            .externalId(externalId)
            .name(productNameToUpdate)
            .build();
        Product updatedProduct = Product.builder()
            .externalId(externalId)
            .name(productNameToUpdate)
            .build();
        when(productRepository.save(productToUpdate)).thenReturn(updatedProduct);

        ProductResponse expectedProductResponse = ProductResponse.builder()
            .id(externalId)
            .name(productNameToUpdate)
            .build();
        when(productResponseMapper.toDto(updatedProduct)).thenReturn(expectedProductResponse);

        // WHEN
        ProductResponse productResponse = defaultProductService.update(externalId, productRequest);

        // THEN
        assertEquals(expectedProductResponse, productResponse);
    }

    @Test
    void givenOffsetLimitAndSortOrders_whenList_thenReturnAListOfProductResponses() {
        // GIVEN
        int offset = 0;
        int limit = 42;
        Pageable pageable = PageRequest.of(offset, limit);

        String externalId = "amazingExternalId";
        Product product = Product.builder()
            .externalId(externalId)
            .build();
        Page<Product> products = new PageImpl<>(
            List.of(
                product
            )
        );
        when(productRepository.findAll(pageable)).thenReturn(products);

        ProductResponse expectedProductResponse = ProductResponse.builder()
            .id(externalId)
            .build();
        when(productResponseMapper.toDto(product)).thenReturn(expectedProductResponse);

        List<ProductResponse> expectedProductResponses = List.of(
            expectedProductResponse
        );

        // WHEN
        Page<ProductResponse> productResponses = defaultProductService.list(offset, limit);

        // THEN
        assertEquals(expectedProductResponses, productResponses.get().collect(Collectors.toList()));
    }

    @Test
    void givenProductId_whenGet_thenReturnProductResponse() {
        // GIVEN
        String externalId = "amazingExternalId";
        Product product = Product.builder()
            .externalId(externalId)
            .build();
        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(product));

        ProductResponse expectedProductResponse = ProductResponse.builder()
            .id(externalId)
            .build();
        when(productResponseMapper.toDto(product)).thenReturn(expectedProductResponse);

        // WHEN
        ProductResponse productResponse = defaultProductService.get(externalId);

        // THEN
        assertEquals(expectedProductResponse, productResponse);
    }

    @Test
    void givenProductId_whenDelete_thenCallRepositoryMethods() {
        // GIVEN
        String externalId = "amazingExternalId";

        Product product = Product.builder()
            .externalId(externalId)
            .build();
        when(productRepository.findByExternalId(externalId)).thenReturn(Optional.of(product));

        // WHEN
        defaultProductService.delete(externalId);

        // THEN
        verify(productRepository, times(1)).findByExternalId(externalId);
        verify(productRepository, times(1)).deleteByExternalId(externalId);
    }

    @Test
    void givenSaveFails_whenCreateProduct_thenEventIsNotPublished() {
        // GIVEN
        ProductRequest productRequest = ProductRequest.builder()
            .build();
        Product productToSave = Product.builder()
            .build();

        when(productRequestMapper.toEntity(productRequest)).thenReturn(productToSave);

        when(productRepository.save(productToSave))
            .thenThrow(new IllegalArgumentException("Transaction failed"));

        // WHEN and THEN
        assertThrows(IllegalArgumentException.class, () -> {
            defaultProductService.create(productRequest);
        });
        verify(applicationEventPublisher, times(0)).publishEvent(any(ProductCreatedEvent.class));
    }
}