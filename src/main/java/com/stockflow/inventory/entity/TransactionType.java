package com.stockflow.inventory.entity;

import com.stockflow.inventory.constants.InventoryConstants;

/**
 * Jenis mutasi stok. Disimpan sebagai string pada {@code inventory_transactions.transaction_type}.
 *
 * <p>Nama event outbox ditempelkan pada konstanta enum, bukan pada {@code switch} di mapper:
 * menambah jenis mutasi baru cukup mengubah satu tempat, tanpa mengubah mapper (OCP).
 */
public enum TransactionType {
    IN(InventoryConstants.EVENT_STOCK_IN_COMPLETED),
    OUT(InventoryConstants.EVENT_STOCK_OUT_COMPLETED),
    ADJUSTMENT(InventoryConstants.EVENT_STOCK_ADJUSTED),
    TRANSFER_IN(InventoryConstants.EVENT_STOCK_TRANSFERRED),
    TRANSFER_OUT(InventoryConstants.EVENT_STOCK_TRANSFERRED);

    private final String eventType;

    TransactionType(String eventType) {
        this.eventType = eventType;
    }

    /** Nama tipe event outbox untuk mutasi ini (FSD §5.3). */
    public String eventType() {
        return eventType;
    }
}
