package com.stockflow.common.response;

/**
 * Envelope response sukses: {@code { "success": true, "message": "...", "data": { } }}.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}
