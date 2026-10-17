package com.stockflow.common.exception;

import java.util.UUID;

/**
 * Posisi stok untuk kombinasi produk dan gudang tidak ditemukan → HTTP 404 (FSD FS-09).
 */
public class InventoryNotFoundException extends BusinessException {

    public InventoryNotFoundException(UUID productId, UUID warehouseId) {
        super(ErrorCode.INVENTORY_NOT_FOUND,
                "Stok untuk produk %s di gudang %s tidak ditemukan".formatted(productId, warehouseId));
    }
}
