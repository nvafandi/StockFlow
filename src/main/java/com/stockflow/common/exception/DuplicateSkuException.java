package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super(ErrorCode.DUPLICATE_SKU, "SKU sudah dipakai: " + sku);
    }
}
