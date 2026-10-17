package com.stockflow.inventory.service;

import com.stockflow.inventory.api.dto.response.StockMutationResponse;

/**
 * Hasil use case mutasi stok untuk API layer.
 *
 * <p>{@code reused = true} berarti {@code Idempotency-Key} sudah pernah dipakai dan tidak ada
 * mutasi baru (FSD §6.1): API layer memetakannya ke HTTP {@code 200 OK}, sedangkan mutasi baru
 * ke {@code 201 Created}. Konversi {@code StockMutationResult} → DTO dilakukan
 * {@code InventoryMapper}, sehingga API layer tidak memuat logika pemetaan.
 */
public record StockMutationOutcome(StockMutationResponse response, boolean reused) {
}
