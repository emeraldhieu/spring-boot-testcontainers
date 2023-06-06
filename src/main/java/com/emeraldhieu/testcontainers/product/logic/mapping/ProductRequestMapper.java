package com.emeraldhieu.testcontainers.product.logic.mapping;

import com.emeraldhieu.testcontainers.product.logic.Product;
import com.emeraldhieu.testcontainers.product.logic.ProductRequest;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link Product} and its DTO {@link ProductRequest}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ProductRequestMapper extends RequestMapper<ProductRequest, Product> {

}