package com.stockflow.inventory.api.rest;

import com.stockflow.common.api.ApiResponse;
import com.stockflow.common.constants.CommonConstants;
import com.stockflow.common.api.PageResponse;
import com.stockflow.inventory.api.dto.request.StockInRequest;
import com.stockflow.inventory.api.dto.request.StockOutRequest;
import com.stockflow.inventory.api.dto.response.InventoryResponse;
import com.stockflow.inventory.api.dto.response.InventoryTransactionResponse;
import com.stockflow.inventory.api.dto.response.StockMutationResponse;
import com.stockflow.inventory.constants.InventoryConstants;
import com.stockflow.inventory.service.InventoryService;
import com.stockflow.inventory.service.StockMutationOutcome;
import jakarta.enterprise.context.ApplicationScoped;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

/**
 * Endpoint inventory: mutasi stok, histori, dan posisi stok.
 *
 * <p>Endpoint mutasi bersifat idempotent melalui header {@code Idempotency-Key}
 * (FSD FS-07–FS-09); status {@code 201} untuk mutasi baru dan {@code 200} untuk pemakaian ulang
 * key yang sama.
 */
@ApplicationScoped
@Path("/api/v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Inventory", description = "Mutasi stok, histori, dan posisi stok")
public class InventoryResource {

    private final InventoryService inventoryService;

    public InventoryResource(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** Menerima stok masuk. */
    @POST
    @Path("/stock-in")
    @Operation(summary = "Stock in", description = "Menambah stok; wajib mengirim header Idempotency-Key.")
    @APIResponse(responseCode = "201", description = "Stok masuk tercatat")
    @APIResponse(responseCode = "200", description = "Idempotency-Key sudah pernah dipakai; hasil lama dikembalikan")
    @APIResponse(responseCode = "400", description = "Request atau header tidak valid")
    @APIResponse(responseCode = "404", description = "Produk/gudang tidak ditemukan")
    @APIResponse(responseCode = "409", description = "Idempotency-Key dipakai dengan payload berbeda")
    public RestResponse<ApiResponse<StockMutationResponse>> stockIn(
            @HeaderParam(InventoryConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid StockInRequest request) {
        StockMutationOutcome result = inventoryService.stockIn(idempotencyKey, request);
        return mutationResponse("Stock received successfully", result);
    }

    /** Mengeluarkan stok. */
    @POST
    @Path("/stock-out")
    @Operation(summary = "Stock out", description = "Mengurangi stok; wajib mengirim header Idempotency-Key.")
    @APIResponse(responseCode = "201", description = "Stok keluar tercatat")
    @APIResponse(responseCode = "200", description = "Idempotency-Key sudah pernah dipakai; hasil lama dikembalikan")
    @APIResponse(responseCode = "400", description = "Request atau header tidak valid")
    @APIResponse(responseCode = "404", description = "Produk/gudang/inventory tidak ditemukan")
    @APIResponse(responseCode = "409", description = "Stok tidak mencukupi atau key dipakai dengan payload berbeda")
    public RestResponse<ApiResponse<StockMutationResponse>> stockOut(
            @HeaderParam(InventoryConstants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid StockOutRequest request) {
        StockMutationOutcome result = inventoryService.stockOut(idempotencyKey, request);
        return mutationResponse("Stock issued successfully", result);
    }

    /** Daftar posisi stok dengan paging. */
    @GET
    @Operation(summary = "Daftar posisi stok", description = "Paging berbasis page/size, maksimal 100 per halaman.")
    @APIResponse(responseCode = "200", description = "Daftar posisi stok")
    @APIResponse(responseCode = "400", description = "Parameter paging tidak valid")
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @QueryParam("page") @DefaultValue(CommonConstants.DEFAULT_PAGE)
            @Min(CommonConstants.MIN_PAGE) int page,
            @QueryParam("size") @DefaultValue(CommonConstants.DEFAULT_PAGE_SIZE)
            @Min(CommonConstants.MIN_PAGE_SIZE) @Max(CommonConstants.MAX_PAGE_SIZE) int size) {
        return ApiResponse.ok("Daftar stok", inventoryService.list(page, size));
    }

    /** Histori transaksi stok (append-only), terbaru lebih dulu. */
    @GET
    @Path("/transactions")
    @Operation(summary = "Histori transaksi stok",
            description = "Dapat difilter per produk dan/atau gudang; terbaru lebih dulu.")
    @APIResponse(responseCode = "200", description = "Histori transaksi stok")
    @APIResponse(responseCode = "400", description = "Parameter paging tidak valid")
    public ApiResponse<PageResponse<InventoryTransactionResponse>> history(
            @QueryParam("productId") UUID productId,
            @QueryParam("warehouseId") UUID warehouseId,
            @QueryParam("page") @DefaultValue(CommonConstants.DEFAULT_PAGE)
            @Min(CommonConstants.MIN_PAGE) int page,
            @QueryParam("size") @DefaultValue(CommonConstants.DEFAULT_PAGE_SIZE)
            @Min(CommonConstants.MIN_PAGE_SIZE) @Max(CommonConstants.MAX_PAGE_SIZE) int size) {
        return ApiResponse.ok("Histori transaksi stok",
                inventoryService.history(productId, warehouseId, page, size));
    }

    /** Posisi stok satu produk pada satu gudang. */
    @GET
    @Path("/{productId}/{warehouseId}")
    @Operation(summary = "Posisi stok per produk dan gudang")
    @APIResponse(responseCode = "200", description = "Posisi stok")
    @APIResponse(responseCode = "404", description = "Posisi stok tidak ditemukan (INVENTORY_NOT_FOUND)")
    public ApiResponse<InventoryResponse> getStock(@PathParam("productId") UUID productId,
                                                   @PathParam("warehouseId") UUID warehouseId) {
        return ApiResponse.ok("Stok per produk dan gudang", inventoryService.getStock(productId, warehouseId));
    }

    private RestResponse<ApiResponse<StockMutationResponse>> mutationResponse(String message, StockMutationOutcome result) {
        // Key baru → 201 Created; key yang sudah pernah dipakai → 200 OK tanpa mutasi baru (FSD FS-07).
        Response.Status status = result.reused() ? Response.Status.OK : Response.Status.CREATED;
        RestResponse.ResponseBuilder<ApiResponse<StockMutationResponse>> response =
                RestResponse.ResponseBuilder.create(status);
        return response
                .entity(ApiResponse.ok(message, result.response()))
                .build();
    }
}
