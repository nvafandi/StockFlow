package com.stockflow.common.exception;

import com.stockflow.common.api.ErrorResponse;
import com.stockflow.common.api.TraceId;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;
import java.util.Map;

/**
 * <b>Satu-satunya penanganan exception untuk seluruh API.</b>
 *
 * <p>Semua handler berkumpul di sini karena bentuk response error sudah seragam
 * ({@link ErrorResponse}) — pemisahan menjadi beberapa mapper hanya menambah file tanpa
 * menambah perilaku. Empat kategori yang ditangani:
 *
 * <ol>
 *   <li>{@link DomainException} → status diambil dari {@link ErrorCode}; penambahan exception
 *       domain baru tidak memerlukan perubahan di kelas ini;
 *   <li>Bean Validation → {@code 400 VALIDATION_ERROR} beserta detail per field;
 *   <li>pelanggaran constraint database → kode domain yang tepat (mis. {@code DUPLICATE_SKU}),
 *       sebagai jaring pengaman bila repository belum sempat menerjemahkannya;
 *   <li>{@link Throwable} → {@code 500 INTERNAL_ERROR} dengan pesan tersanitasi: detail
 *       internal hanya masuk log (FSD §9.2 butir 2).
 * </ol>
 *
 * <p>Setiap response error selalu membawa header {@code X-Correlation-Id}.
 */
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    /**
     * Pemetaan nama constraint database → kode domain. Berupa tabel, bukan {@code switch}:
     * menambah constraint baru cukup menambah satu entri (OCP).
     */
    private static final Map<String, ErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
            "uk_products_sku", ErrorCode.DUPLICATE_SKU,
            "uk_warehouses_code", ErrorCode.DUPLICATE_CODE,
            "uk_inventory_transactions_idempotency_key", ErrorCode.IDEMPOTENCY_KEY_REUSED,
            "uk_inventory_product_warehouse", ErrorCode.CONCURRENCY_CONFLICT,
            "ck_inventory_quantity_non_negative", ErrorCode.CONCURRENCY_CONFLICT);

    /** Exception domain: status HTTP ditentukan oleh {@link ErrorCode}. */
    @ServerExceptionMapper
    public Response handleDomain(DomainException exception, ContainerRequestContext request) {
        ErrorCode code = exception.errorCode();
        String traceId = TraceId.resolve(request);
        LOG.warnf("traceId=%s code=%s message=%s", traceId, code.code(), exception.getMessage());
        return error(code, exception.getMessage(), traceId, List.of());
    }

    /** Kegagalan Bean Validation (body request maupun parameter) → 400. */
    @ServerExceptionMapper
    public Response handleValidation(jakarta.validation.ConstraintViolationException exception,
                                     ContainerRequestContext request) {
        List<ErrorResponse.FieldDetail> details = exception.getConstraintViolations().stream()
                .map(this::toFieldDetail)
                .toList();

        String traceId = TraceId.resolve(request);
        String message = details.isEmpty()
                ? "Request tidak valid"
                : details.getFirst().field() + ": " + details.getFirst().message();

        return error(ErrorCode.VALIDATION_ERROR, message, traceId, details);
    }

    /**
     * Pelanggaran constraint database yang belum diterjemahkan repository (mis. karena balapan
     * request). Tanpa handler ini pelanggaran unique akan muncul sebagai {@code 500}, padahal
     * kontrak API menuntut kode domain yang tepat.
     */
    @ServerExceptionMapper
    public Response handleDatabaseConstraint(org.hibernate.exception.ConstraintViolationException exception,
                                             ContainerRequestContext request) {
        ErrorCode code = translate(exception.getConstraintName());
        String traceId = TraceId.resolve(request);
        String message = "Pelanggaran constraint database: " + code.code();
        LOG.warnf(exception, "traceId=%s constraint=%s code=%s", traceId, exception.getConstraintName(), code.code());

        return error(code, message, traceId, List.of());
    }

    /** Fallback: exception tak terduga → 500 dengan pesan generik. */
    @ServerExceptionMapper
    public Response handleUnexpected(Throwable exception, ContainerRequestContext request) {
        if (exception instanceof WebApplicationException web) {
            return web.getResponse();
        }

        String traceId = TraceId.resolve(request);
        LOG.errorf(exception, "traceId=%s unhandled exception", traceId);

        return error(ErrorCode.INTERNAL_ERROR, "Terjadi kesalahan internal", traceId, List.of());
    }

    private Response error(ErrorCode code, String message, String traceId, List<ErrorResponse.FieldDetail> details) {
        return Response.status(code.httpStatus())
                .header(TraceId.HEADER, traceId)
                .entity(ErrorResponse.of(message, code, traceId, details))
                .build();
    }

    private ErrorResponse.FieldDetail toFieldDetail(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return new ErrorResponse.FieldDetail(field, violation.getMessage());
    }

    private ErrorCode translate(String constraintName) {
        if (constraintName == null) {
            return ErrorCode.CONCURRENCY_CONFLICT;
        }
        return CONSTRAINT_ERROR_CODES.getOrDefault(constraintName, ErrorCode.CONCURRENCY_CONFLICT);
    }
}
