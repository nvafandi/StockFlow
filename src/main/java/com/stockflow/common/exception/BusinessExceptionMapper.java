package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;
import com.stockflow.common.response.ErrorResponse;
import com.stockflow.common.response.TraceId;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Satu mapper global untuk seluruh exception domain.
 *
 * <p>HTTP status diambil dari {@link ErrorCode} sehingga penambahan exception baru tidak
 * memerlukan mapper tambahan.
 */
@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    private static final Logger LOG = Logger.getLogger(BusinessExceptionMapper.class);

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(BusinessException exception) {
        ErrorCode code = exception.errorCode();
        String traceId = TraceId.resolve(headers);
        LOG.warnf("traceId=%s code=%s message=%s", traceId, code.code(), exception.getMessage());

        return Response.status(code.httpStatus())
                .header(TraceId.HEADER, traceId)
                .entity(ErrorResponse.of(exception.getMessage(), code, traceId, List.of()))
                .build();
    }
}
