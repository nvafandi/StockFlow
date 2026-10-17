package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

import java.util.UUID;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(UUID productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Produk tidak ditemukan: " + productId);
    }
}
