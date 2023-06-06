package com.emeraldhieu.testcontainers.product.logic.mapping;

import com.emeraldhieu.testcontainers.product.logic.Product;
import com.emeraldhieu.testcontainers.product.logic.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mapper for the entity {@link Product} and its DTO {@link ProductResponse}.
 */
@Mapper(componentModel = "spring")
public interface ProductResponseMapper extends ResponseMapper<ProductResponse, Product> {

    @Mappings({
        @Mapping(source = "externalId", target = "id")
    })
    @Override
    ProductResponse toDto(Product entity);
}