package com.stockflow.common.exception;

/**
 * Exception domain generik untuk kondisi yang tidak punya tipe khusus
 * (mis. validasi header wajib pada {@code InventoryService}).
 *
 * <p>Selalu mengembalikan {@link ErrorCode} sehingga {@link GlobalExceptionHandler} dapat
 * memetakannya ke HTTP status dan body error standar.
 */
public class BusinessException extends DomainException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
