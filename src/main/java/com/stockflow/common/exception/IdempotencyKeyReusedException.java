package com.stockflow.common.exception;

/**
 * {@code Idempotency-Key} sudah dipakai untuk payload yang berbeda → HTTP 409 (FSD §6.1).
 */
public class IdempotencyKeyReusedException extends BusinessException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "Idempotency-Key sudah dipakai untuk request dengan payload berbeda: " + idempotencyKey);
    }
}
