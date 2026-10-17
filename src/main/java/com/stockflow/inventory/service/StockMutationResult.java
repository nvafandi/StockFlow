package com.stockflow.inventory.service;

import com.stockflow.inventory.entity.InventoryTransaction;

import java.util.UUID;

/**
 * Hasil satu mutasi stok, dipakai sebagai dasar response API.
 *
 * <p>{@code reused = true} berarti {@code Idempotency-Key} sudah pernah dipakai dan tidak
 * ada mutasi baru yang dijalankan (FSD §6.1 baris 2 → HTTP 200).
 */
public record StockMutationResult(
        UUID transactionId,
        UUID eventId,
        UUID inventoryId,
        UUID productId,
        UUID warehouseId,
        long quantityBefore,
        long quantityChanged,
        long quantityAfter,
        boolean reused) {

    public static StockMutationResult of(UUID transactionId, UUID eventId, UUID inventoryId,
                                         UUID productId, UUID warehouseId,
                                         long quantityBefore, long quantityChanged, long quantityAfter) {
        return new StockMutationResult(transactionId, eventId, inventoryId, productId, warehouseId,
                quantityBefore, quantityChanged, quantityAfter, false);
    }

    public static StockMutationResult reused(InventoryTransaction transaction) {
        return new StockMutationResult(
                transaction.getId(),
                null,
                null,
                transaction.getProductId(),
                transaction.getWarehouseId(),
                transaction.getQuantityBefore(),
                transaction.getQuantityDelta(),
                transaction.getQuantityAfter(),
                true);
    }
}
