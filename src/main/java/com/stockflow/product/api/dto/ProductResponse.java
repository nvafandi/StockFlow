package com.stockflow.product.api.dto;

import com.stockflow.product.domain.Product;

import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, String description, String unit, boolean active) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id, product.sku, product.name, product.description, product.unit, product.active);
    }
}
