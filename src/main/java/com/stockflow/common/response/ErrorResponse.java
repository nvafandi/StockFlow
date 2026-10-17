package com.stockflow.common.response;

import java.util.List;

/**
 * Envelope response gagal.
 *
 * <pre>
 * {
 *   "success": false,
 *   "message": "Insufficient stock ...",
 *   "error": { "code": "INSUFFICIENT_STOCK", "details": [], "traceId": "..." }
 * }
 * </pre>
 */
public record ErrorResponse(boolean success, String message, ErrorDetail error) {

    public record ErrorDetail(String code, List<FieldDetail> details, String traceId) {
    }

    public record FieldDetail(String field, String message) {
    }

    public static ErrorResponse of(String message, ErrorCode code, String traceId, List<FieldDetail> details) {
        return new ErrorResponse(false, message, new ErrorDetail(code.code(), details, traceId));
    }
}
