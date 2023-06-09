package com.emeraldhieu.testcontainers.product.logic;

import com.emeraldhieu.testcontainers.product.logic.mapping.ProductRequestMapper;
import com.emeraldhieu.testcontainers.product.logic.mapping.ProductResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DefaultProductService implements ProductService {

    private final ProductRepository productRepository;
    private final ProductRequestMapper productRequestMapper;
    private final ProductResponseMapper productResponseMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest productRequest) {
        Product productToSave = productRequestMapper.toEntity(productRequest);
        Product savedProduct = productRepository.save(productToSave);
        sendEvent(savedProduct);
        return productResponseMapper.toDto(savedProduct);
    }

    private void sendEvent(Product product) {
        log.info("Sending %s...".formatted(ProductCreatedEvent.class.getSimpleName()));
        ProductCreatedEvent event = ProductCreatedEvent.builder()
            .id(product.getExternalId())
            .name(product.getName())
            .price(product.getPrice())
            .build();
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    @Transactional
    public ProductResponse update(String id, ProductRequest productRequest) {
        Product productToUpdate = productRepository.findByExternalId(id)
            .map(currentProduct -> {
                productRequestMapper.partialUpdate(currentProduct, productRequest);
                return currentProduct;
            })
            .orElseThrow(() -> new ProductNotFoundException(id));
        Product updatedProduct = productRepository.save(productToUpdate);
        return productResponseMapper.toDto(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(int offset, int limit) {
        Pageable pageable = PageRequest.of(offset, limit);
        return productRepository.findAll(pageable)
            .map(productResponseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse get(String id) {
        return productRepository.findByExternalId(id)
            .map(productResponseMapper::toDto)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional
    public void delete(String id) {
        productRepository.findByExternalId(id)
            .ifPresent(product -> productRepository.deleteByExternalId(id));
    }
}
