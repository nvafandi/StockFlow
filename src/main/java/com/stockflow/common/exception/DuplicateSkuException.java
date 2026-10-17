package com.stockflow.common.exception;

/**
 * SKU produk sudah dipakai (unique constraint {@code uk_products_sku}) → HTTP 409.
 */
public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super(ErrorCode.DUPLICATE_SKU, "SKU sudah dipakai: " + sku);
    }
}
