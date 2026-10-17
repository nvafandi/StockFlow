package com.stockflow.inventory.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Satu baris histori transaksi stok. Konversi dilakukan {@code InventoryMapper}.
 */
public record InventoryTransactionResponse(
        UUID transactionId,
        String transactionType,
        long quantityDelta,
        long quantityBefore,
        long quantityAfter,
        String referenceId,
        OffsetDateTime createdAt) {
}
