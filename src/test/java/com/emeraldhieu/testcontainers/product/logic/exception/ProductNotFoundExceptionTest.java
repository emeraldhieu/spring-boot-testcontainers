package com.emeraldhieu.testcontainers.product.logic.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductNotFoundExceptionTest {

    private ProductNotFoundException exception;

    @Test
    public void givenProductId_whenCreateProductNotFoundException_thenReturnAnExceptionWithProductId() {
        // GIVEN
        String productId = "product42";

        // WHEN
        exception = new ProductNotFoundException(productId);

        // THEN
        assertEquals(productId, exception.getProductId());
    }
}