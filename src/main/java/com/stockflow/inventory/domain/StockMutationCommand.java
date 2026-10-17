package com.stockflow.inventory.domain;

import java.util.UUID;

/**
 * Perintah mutasi stok yang sudah tervalidasi di level DTO dan siap dieksekusi repository.
 *
 * <p>{@code payloadHash} adalah sha256 dari payload request; dipakai untuk mendeteksi
 * pemakaian ulang {@code Idempotency-Key} dengan isi berbeda (FSD §6.2).
 */
public record StockMutationCommand(
        UUID productId,
        UUID warehouseId,
        long quantity,
        String referenceId,
        String idempotencyKey,
        String notes,
        String payloadHash) {

    public static StockMutationCommand of(UUID productId, UUID warehouseId, long quantity,
                                          String referenceId, String idempotencyKey, String notes) {
        String payloadHash = PayloadHasher.sha256(
                "%s|%s|%d|%s|%s".formatted(productId, warehouseId, quantity, nullSafe(referenceId), nullSafe(notes)));
        return new StockMutationCommand(
                productId, warehouseId, quantity, referenceId, idempotencyKey, notes, payloadHash);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
