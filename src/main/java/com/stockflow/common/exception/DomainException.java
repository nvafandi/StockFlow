package com.stockflow.common.exception;

/**
 * Akar seluruh exception domain StockFlow.
 *
 * <p>Membawa {@link ErrorCode} sehingga satu {@link GlobalExceptionHandler} dapat memetakan
 * exception apa pun ke HTTP status dan body error standar tanpa mapper per exception.
 *
 * <p>Kelas ini abstrak: setiap kondisi gagal harus punya tipe exception sendiri
 * (mis. {@code InsufficientStockException}); gunakan {@link BusinessException} hanya bila
 * memang tidak ada tipe yang lebih spesifik.
 */
public abstract class DomainException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
