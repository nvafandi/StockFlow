package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

public class IdempotencyKeyReusedException extends BusinessException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "Idempotency-Key sudah dipakai untuk request dengan payload berbeda: " + idempotencyKey);
    }
}
