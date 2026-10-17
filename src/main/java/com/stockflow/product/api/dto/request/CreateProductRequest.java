package com.stockflow.product.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body request pembuatan produk (FSD FS-01).
 */
public record CreateProductRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 30) String unit) {
}
