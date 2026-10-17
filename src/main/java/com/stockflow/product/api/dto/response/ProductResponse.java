package com.stockflow.product.api.dto.response;

import java.util.UUID;

/**
 * Representasi produk pada API. Konversi dari entity dilakukan {@code ProductMapper}.
 */
public record ProductResponse(UUID id, String sku, String name, String description, String unit, boolean active) {
}
