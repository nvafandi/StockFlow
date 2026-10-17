package com.stockflow.inventory.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockInRequest(
        @NotNull(message = "productId wajib diisi") UUID productId,
        @NotNull(message = "warehouseId wajib diisi") UUID warehouseId,
        @Positive(message = "quantity harus lebih besar dari 0") long quantity,
        @Size(max = 100, message = "referenceId maksimal 100 karakter") String referenceId,
        String notes) {
}
