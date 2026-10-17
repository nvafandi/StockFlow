package com.stockflow.common.exception;

/**
 * Kode error standar yang dipakai pada seluruh API.
 *
 * <p>Setiap kode membawa HTTP status-nya masing-masing sehingga exception domain cukup
 * menyebut kode dan tidak perlu mengetahui detail HTTP.
 */
public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", 400),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", 404),
    WAREHOUSE_NOT_FOUND("WAREHOUSE_NOT_FOUND", 404),
    INVENTORY_NOT_FOUND("INVENTORY_NOT_FOUND", 404),
    DUPLICATE_SKU("DUPLICATE_SKU", 409),
    DUPLICATE_CODE("DUPLICATE_CODE", 409),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", 409),
    IDEMPOTENCY_KEY_REUSED("IDEMPOTENCY_KEY_REUSED", 409),
    CONCURRENCY_CONFLICT("CONCURRENCY_CONFLICT", 409),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", 422),
    INTERNAL_ERROR("INTERNAL_ERROR", 500);

    private final String code;
    private final int httpStatus;

    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
