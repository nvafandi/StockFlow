package com.stockflow.common.api;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.UUID;

/**
 * Mengambil {@code X-Correlation-Id} dari request; bila tidak dikirim, nilai baru dibuat
 * sehingga setiap error selalu dapat ditelusuri.
 */
public final class TraceId {

    public static final String HEADER = "X-Correlation-Id";

    private TraceId() {
    }

    public static String resolve(ContainerRequestContext requestContext) {
        if (requestContext != null) {
            String value = requestContext.getHeaderString(HEADER);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
