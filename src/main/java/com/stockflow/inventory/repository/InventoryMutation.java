package com.stockflow.inventory.repository;

import java.util.UUID;

/**
 * Proyeksi hasil satu statement mutasi {@code inventory}.
 *
 * <p>Dikembalikan oleh {@link InventoryRepository} dan dikonsumsi oleh service layer
 * ({@code InventoryMutationService}); bukan entity.
 */
public record InventoryMutation(UUID inventoryId, long quantityAfter) {
}
