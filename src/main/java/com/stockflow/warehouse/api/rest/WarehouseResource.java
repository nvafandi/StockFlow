package com.stockflow.warehouse.api.rest;

import com.stockflow.common.api.ApiResponse;
import com.stockflow.common.constants.CommonConstants;
import com.stockflow.common.api.PageResponse;
import com.stockflow.warehouse.api.dto.request.CreateWarehouseRequest;
import com.stockflow.warehouse.api.dto.response.WarehouseResponse;
import com.stockflow.warehouse.service.WarehouseService;
import jakarta.enterprise.context.ApplicationScoped;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoint master data gudang.
 *
 * <p>Resource hanya memvalidasi bentuk request dan mendelegasikan ke application service
 * (FSD FS-04–FS-06).
 */
@ApplicationScoped
@Path("/api/v1/warehouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Gudang", description = "Master data gudang")
public class WarehouseResource {

    private final WarehouseService warehouseService;

    public WarehouseResource(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    /** Membuat gudang baru. Response sukses selalu {@code 201 Created} + header {@code Location}. */
    @POST
    @Operation(summary = "Membuat gudang baru", description = "Kode gudang harus unik dan belum pernah dipakai.")
    @APIResponse(responseCode = "201", description = "Gudang berhasil dibuat")
    @APIResponse(responseCode = "400", description = "Request tidak valid (Bean Validation)")
    @APIResponse(responseCode = "409", description = "Kode gudang sudah dipakai (DUPLICATE_CODE)")
    public RestResponse<ApiResponse<WarehouseResponse>> create(@Valid CreateWarehouseRequest request) {
        WarehouseResponse created = warehouseService.create(request);
        RestResponse.ResponseBuilder<ApiResponse<WarehouseResponse>> response =
                RestResponse.ResponseBuilder.create(Response.Status.CREATED);
        return response
                .header("Location", URI.create("/api/v1/warehouses/" + created.id()))
                .entity(ApiResponse.ok("Gudang berhasil dibuat", created))
                .build();
    }

    /** Daftar gudang dengan paging. */
    @GET
    @Operation(summary = "Daftar gudang", description = "Paging berbasis page/size, maksimal 100 per halaman.")
    @APIResponse(responseCode = "200", description = "Daftar gudang")
    @APIResponse(responseCode = "400", description = "Parameter paging tidak valid")
    public ApiResponse<PageResponse<WarehouseResponse>> list(
            @QueryParam("page") @DefaultValue(CommonConstants.DEFAULT_PAGE)
            @Min(CommonConstants.MIN_PAGE) int page,
            @QueryParam("size") @DefaultValue(CommonConstants.DEFAULT_PAGE_SIZE)
            @Min(CommonConstants.MIN_PAGE_SIZE) @Max(CommonConstants.MAX_PAGE_SIZE) int size) {
        return ApiResponse.ok("Daftar gudang", warehouseService.list(page, size));
    }

    /** Detail satu gudang. */
    @GET
    @Path("/{id}")
    @Operation(summary = "Detail gudang")
    @APIResponse(responseCode = "200", description = "Detail gudang")
    @APIResponse(responseCode = "404", description = "Gudang tidak ditemukan (WAREHOUSE_NOT_FOUND)")
    public ApiResponse<WarehouseResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok("Detail gudang", warehouseService.get(id));
    }
}
