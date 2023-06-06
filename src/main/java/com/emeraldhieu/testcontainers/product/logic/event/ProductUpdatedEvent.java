package com.emeraldhieu.testcontainers.product.logic.event;

import com.emeraldhieu.testcontainers.product.logic.ProductEventListener;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * @see ProductEventListener
 */
@Builder(toBuilder = true)
@Getter
@Jacksonized
@EqualsAndHashCode
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ProductUpdatedEvent {
    private final String id;
    private final String name;
    private final double price;
}
