package com.stockflow.inventory.api.dto.response;

import java.util.UUID;

/**
 * Bentuk response FS-07 / FS-08. Konversi dilakukan {@code InventoryMapper}.
 */
public record StockMutationResponse(
        UUID productId,
        UUID warehouseId,
        long quantityBefore,
        long quantityChanged,
        long quantityAfter,
        UUID transactionId) {
}
