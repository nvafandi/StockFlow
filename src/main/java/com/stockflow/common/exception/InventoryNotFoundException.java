package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

import java.util.UUID;

public class InventoryNotFoundException extends BusinessException {

    public InventoryNotFoundException(UUID productId, UUID warehouseId) {
        super(ErrorCode.INVENTORY_NOT_FOUND,
                "Stok untuk produk %s di gudang %s tidak ditemukan".formatted(productId, warehouseId));
    }
}
