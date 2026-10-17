package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;
import com.stockflow.common.response.ErrorResponse;
import com.stockflow.common.response.TraceId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Memetakan kegagalan Bean Validation (body request maupun parameter) ke
 * {@code 400 VALIDATION_ERROR} beserta detail per field.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErrorResponse.FieldDetail> details = exception.getConstraintViolations().stream()
                .map(this::toFieldDetail)
                .toList();

        String traceId = TraceId.resolve(headers);
        String message = details.isEmpty()
                ? "Request tidak valid"
                : details.getFirst().field() + ": " + details.getFirst().message();

        return Response.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .header(TraceId.HEADER, traceId)
                .entity(ErrorResponse.of(message, ErrorCode.VALIDATION_ERROR, traceId, details))
                .build();
    }

    private ErrorResponse.FieldDetail toFieldDetail(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return new ErrorResponse.FieldDetail(field, violation.getMessage());
    }
}
