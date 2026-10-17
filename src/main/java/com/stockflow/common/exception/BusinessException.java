package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

/**
 * Basis seluruh exception domain.
 *
 * <p>Selalu mengembalikan {@link ErrorCode} sehingga satu exception mapper global dapat
 * memetakannya ke HTTP status dan body error standar.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
