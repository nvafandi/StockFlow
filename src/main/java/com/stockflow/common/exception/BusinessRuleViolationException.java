package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;

public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
