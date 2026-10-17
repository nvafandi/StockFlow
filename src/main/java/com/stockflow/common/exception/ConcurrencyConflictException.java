package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

public class ConcurrencyConflictException extends BusinessException {

    public ConcurrencyConflictException(String detail) {
        super(ErrorCode.CONCURRENCY_CONFLICT, "Konflik konkurensi, silakan ulangi request: " + detail);
    }
}
