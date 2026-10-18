package com.stockflow.common.exception;

import java.util.UUID;

/**
 * Gudang tidak ditemukan berdasarkan {@code id} → HTTP 404 (FSD FS-06).
 */
public class WarehouseNotFoundException extends BusinessException {

    public WarehouseNotFoundException(UUID warehouseId) {
        super(ErrorCode.WAREHOUSE_NOT_FOUND, "Gudang tidak ditemukan: " + warehouseId);
    }
}
