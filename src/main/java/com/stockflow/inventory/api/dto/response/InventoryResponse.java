package com.stockflow.inventory.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Posisi stok satu produk pada satu gudang. Konversi dilakukan {@code InventoryMapper}.
 */
public record InventoryResponse(
        UUID inventoryId,
        UUID productId,
        UUID warehouseId,
        long quantity,
        OffsetDateTime updatedAt) {
}
