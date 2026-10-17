package com.stockflow.common.exception;

/**
 * Pelanggaran aturan bisnis yang bukan masalah bentuk request maupun duplikasi data
 * (mis. produk atau gudang non-aktif) → HTTP 422.
 */
public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
