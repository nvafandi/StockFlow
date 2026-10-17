package com.stockflow.common.exception;

import java.util.UUID;

/**
 * Stok tidak mencukupi untuk permintaan stock out → HTTP 409 (FSD FS-08).
 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(UUID productId, UUID warehouseId, long requested) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Stok tidak mencukupi untuk produk %s di gudang %s (diminta %d)"
                        .formatted(productId, warehouseId, requested));
    }
}
