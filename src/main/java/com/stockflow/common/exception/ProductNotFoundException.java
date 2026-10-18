package com.stockflow.common.exception;

import java.util.UUID;

/**
 * Produk tidak ditemukan berdasarkan {@code id} → HTTP 404 (FSD FS-03).
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(UUID productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Produk tidak ditemukan: " + productId);
    }
}
