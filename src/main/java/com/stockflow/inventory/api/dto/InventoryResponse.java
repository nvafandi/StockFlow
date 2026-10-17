package com.stockflow.inventory.api.dto;

import com.stockflow.inventory.domain.Inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID inventoryId,
        UUID productId,
        UUID warehouseId,
        long quantity,
        OffsetDateTime updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.id, inventory.productId, inventory.warehouseId, inventory.quantity, inventory.updatedAt);
    }
}
