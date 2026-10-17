package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

public class DuplicateCodeException extends BusinessException {

    public DuplicateCodeException(String code) {
        super(ErrorCode.DUPLICATE_CODE, "Kode gudang sudah dipakai: " + code);
    }
}
