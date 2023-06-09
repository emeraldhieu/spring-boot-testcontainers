package com.emeraldhieu.testcontainers.product.logic;

import lombok.Getter;

@Getter
public class ProductNotFoundException extends RuntimeException {

    private final String productId;

    public ProductNotFoundException(String productId) {
        super();
        this.productId = productId;
    }
}
