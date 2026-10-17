package com.stockflow.inventory.api;

import com.stockflow.common.response.ApiResponse;
import com.stockflow.common.response.PageResponse;
import com.stockflow.inventory.api.dto.InventoryResponse;
import com.stockflow.inventory.api.dto.InventoryTransactionResponse;
import com.stockflow.inventory.api.dto.StockInRequest;
import com.stockflow.inventory.api.dto.StockMutationResponse;
import com.stockflow.inventory.api.dto.StockOutRequest;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.inventory.domain.StockMutationResult;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final InventoryService inventoryService;

    @Inject
    public InventoryResource(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @POST
    @Path("/stock-in")
    public Response stockIn(@HeaderParam("Idempotency-Key") String idempotencyKey,
                            @Valid StockInRequest request) {
        StockMutationResult result = inventoryService.stockIn(idempotencyKey, request);
        return mutationResponse("Stock received successfully", result);
    }

    @POST
    @Path("/stock-out")
    public Response stockOut(@HeaderParam("Idempotency-Key") String idempotencyKey,
                             @Valid StockOutRequest request) {
        StockMutationResult result = inventoryService.stockOut(idempotencyKey, request);
        return mutationResponse("Stock issued successfully", result);
    }

    @GET
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Daftar stok", inventoryService.list(page, size));
    }

    @GET
    @Path("/transactions")
    public ApiResponse<PageResponse<InventoryTransactionResponse>> history(
            @QueryParam("productId") UUID productId,
            @QueryParam("warehouseId") UUID warehouseId,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Histori transaksi stok",
                inventoryService.history(productId, warehouseId, page, size));
    }

    @GET
    @Path("/{productId}/{warehouseId}")
    public ApiResponse<InventoryResponse> getStock(@PathParam("productId") UUID productId,
                                                   @PathParam("warehouseId") UUID warehouseId) {
        return ApiResponse.ok("Stok per produk dan gudang", inventoryService.getStock(productId, warehouseId));
    }

    private Response mutationResponse(String message, StockMutationResult result) {
        // Key baru → 201 Created; key yang sudah pernah dipakai → 200 OK tanpa mutasi baru (FSD FS-07).
        Response.Status status = result.reused() ? Response.Status.OK : Response.Status.CREATED;
        return Response.status(status)
                .entity(ApiResponse.ok(message, StockMutationResponse.from(result)))
                .build();
    }
}
