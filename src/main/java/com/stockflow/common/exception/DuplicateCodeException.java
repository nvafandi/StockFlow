package com.stockflow.common.exception;

/**
 * Kode gudang sudah dipakai (unique constraint {@code uk_warehouses_code}) → HTTP 409.
 */
public class DuplicateCodeException extends BusinessException {

    public DuplicateCodeException(String code) {
        super(ErrorCode.DUPLICATE_CODE, "Kode gudang sudah dipakai: " + code);
    }
}
