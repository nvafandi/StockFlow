package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorCode;
import com.stockflow.common.response.ErrorResponse;
import com.stockflow.common.response.TraceId;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Fallback {@code 500 INTERNAL_ERROR}. Detail internal hanya masuk log, tidak ke response
 * (lihat FSD §9.2 butir 2).
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException web) {
            return web.getResponse();
        }

        String traceId = TraceId.resolve(headers);
        LOG.errorf(exception, "traceId=%s unhandled exception", traceId);

        return Response.status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .header(TraceId.HEADER, traceId)
                .entity(ErrorResponse.of("Terjadi kesalahan internal", ErrorCode.INTERNAL_ERROR, traceId, List.of()))
                .build();
    }
}
