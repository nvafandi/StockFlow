package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;
import com.stockflow.common.response.ErrorResponse;
import com.stockflow.common.response.TraceId;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Jaring pengaman terakhir untuk pelanggaran constraint database yang belum sempat
 * diterjemahkan oleh repository menjadi exception domain (mis. karena balapan request).
 *
 * <p>Tanpa mapper ini pelanggaran unique akan muncul sebagai {@code 500}; padahal
 * kontrak API menuntut kode domain yang tepat (mis. {@code DUPLICATE_SKU}).
 */
@Provider
public class DatabaseConstraintExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(DatabaseConstraintExceptionMapper.class);

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        ErrorCode code = translate(exception.getConstraintName());
        String traceId = TraceId.resolve(headers);
        String message = "Pelanggaran constraint database: " + code.code();
        LOG.warnf(exception, "traceId=%s constraint=%s code=%s", traceId, exception.getConstraintName(), code.code());

        return Response.status(code.httpStatus())
                .header(TraceId.HEADER, traceId)
                .entity(ErrorResponse.of(message, code, traceId, List.of()))
                .build();
    }

    private ErrorCode translate(String constraintName) {
        if (constraintName == null) {
            return ErrorCode.CONCURRENCY_CONFLICT;
        }
        return switch (constraintName) {
            case "uk_products_sku" -> ErrorCode.DUPLICATE_SKU;
            case "uk_warehouses_code" -> ErrorCode.DUPLICATE_CODE;
            case "uk_inventory_transactions_idempotency_key" -> ErrorCode.IDEMPOTENCY_KEY_REUSED;
            case "uk_inventory_product_warehouse", "ck_inventory_quantity_non_negative" -> ErrorCode.CONCURRENCY_CONFLICT;
            default -> ErrorCode.CONCURRENCY_CONFLICT;
        };
    }
}
