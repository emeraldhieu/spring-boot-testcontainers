package com.emeraldhieu.testcontainers.product.logic;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An event that is fired when a product is created.
 */
@Builder
@Getter
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ProductCreatedEvent {
    private final String id;
    private final String name;
    private final double price;
}
