package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

import java.util.UUID;

public class WarehouseNotFoundException extends BusinessException {

    public WarehouseNotFoundException(UUID warehouseId) {
        super(ErrorCode.WAREHOUSE_NOT_FOUND, "Gudang tidak ditemukan: " + warehouseId);
    }
}
