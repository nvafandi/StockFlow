package com.stockflow.inventory.infrastructure;

import java.util.UUID;

/**
 * Proyeksi hasil satu statement mutasi inventory.
 *
 * <p>Package-private: hanya dipakai di dalam layer infrastructure modul inventory.
 */
record InventoryMutation(UUID inventoryId, long quantityAfter) {
}
