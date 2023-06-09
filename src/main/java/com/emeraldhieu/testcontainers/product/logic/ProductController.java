package com.emeraldhieu.testcontainers.product.logic;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    static final String PRODUCT_PATTERN = "/products/%s";

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/products",
        produces = {"application/json"},
        consumes = {"application/json"}
    )
    public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody ProductRequest productRequest
    ) {
        ProductResponse createdProduct = productService.create(productRequest);
        return ResponseEntity.created(URI.create(String.format(PRODUCT_PATTERN, createdProduct.getId())))
            .body(createdProduct);
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/products/{id}"
    )
    public ResponseEntity<Void> deleteProduct(
        @PathVariable("id") String id
    ) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/products/{id}",
        produces = {"application/json"}
    )
    public ResponseEntity<ProductResponse> getProduct(
        @PathVariable("id") String id
    ) {
        ProductResponse retrievedProduct = productService.get(id);
        return ResponseEntity.ok(retrievedProduct);
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/products",
        produces = {"application/json"}
    )
    public ResponseEntity<List<ProductResponse>> listProducts(
        @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
        @Valid @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
    ) {
        Page<ProductResponse> productResponsePage = productService.list(offset, limit);
        List<ProductResponse> productResponses = productResponsePage.stream()
            .collect(Collectors.toList());
        return ResponseEntity.ok(productResponses);
    }

    @RequestMapping(
        method = RequestMethod.PATCH,
        value = "/products/{id}",
        produces = {"application/json"},
        consumes = {"application/json"}
    )
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable("id") String id,
        @Valid @RequestBody ProductRequest productRequest
    ) {
        ProductResponse updatedProduct = productService.update(id, productRequest);
        return ResponseEntity.ok(updatedProduct);
    }
}
