package com.stockflow.inventory.domain;

/**
 * Jenis mutasi stok. Disimpan sebagai string pada {@code inventory_transactions.transaction_type}.
 */
public enum TransactionType {
    IN,
    OUT,
    ADJUSTMENT,
    TRANSFER_IN,
    TRANSFER_OUT
}
