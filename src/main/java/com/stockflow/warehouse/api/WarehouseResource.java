package com.stockflow.warehouse.api;

import com.stockflow.common.response.ApiResponse;
import com.stockflow.common.response.PageResponse;
import com.stockflow.warehouse.api.dto.CreateWarehouseRequest;
import com.stockflow.warehouse.api.dto.WarehouseResponse;
import com.stockflow.warehouse.application.WarehouseService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Path("/api/v1/warehouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WarehouseResource {

    private final WarehouseService warehouseService;

    @Inject
    public WarehouseResource(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @POST
    public Response create(@Valid CreateWarehouseRequest request) {
        WarehouseResponse created = warehouseService.create(request);
        return Response.created(URI.create("/api/v1/warehouses/" + created.id()))
                .entity(ApiResponse.ok("Gudang berhasil dibuat", created))
                .build();
    }

    @GET
    public ApiResponse<PageResponse<WarehouseResponse>> list(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Daftar gudang", warehouseService.list(page, size));
    }

    @GET
    @Path("/{id}")
    public ApiResponse<WarehouseResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok("Detail gudang", warehouseService.get(id));
    }
}
