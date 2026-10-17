package com.stockflow.common.response;

import jakarta.ws.rs.core.HttpHeaders;

import java.util.UUID;

/**
 * Mengambil {@code X-Correlation-Id} dari request; bila tidak dikirim, nilai baru dibuat
 * sehingga setiap error selalu dapat ditelusuri.
 */
public final class TraceId {

    public static final String HEADER = "X-Correlation-Id";

    private TraceId() {
    }

    public static String resolve(HttpHeaders headers) {
        if (headers != null) {
            String value = headers.getHeaderString(HEADER);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
