package com.sk.skala.myapp.dto;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;

public record ProductResponse(
        Long id,
        String name,
        Integer price,
        Integer stockQuantity,
        ProductStatus status,
        String description
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getDescription()
        );
    }
}
