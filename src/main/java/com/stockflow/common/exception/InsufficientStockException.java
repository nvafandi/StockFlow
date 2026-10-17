package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

import java.util.UUID;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(UUID productId, UUID warehouseId, long requested) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Stok tidak mencukupi untuk produk %s di gudang %s (diminta %d)"
                        .formatted(productId, warehouseId, requested));
    }
}
