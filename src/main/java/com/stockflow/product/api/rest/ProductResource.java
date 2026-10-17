package com.stockflow.product.api.rest;

import com.stockflow.common.api.ApiResponse;
import com.stockflow.common.constants.CommonConstants;
import com.stockflow.common.api.PageResponse;
import com.stockflow.product.api.dto.request.CreateProductRequest;
import com.stockflow.product.api.dto.response.ProductResponse;
import com.stockflow.product.service.ProductService;
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
 * Endpoint master data produk.
 *
 * <p>Resource hanya memvalidasi bentuk request (Bean Validation) dan mendelegasikan ke
 * application service; tidak ada logika bisnis di sini (FSD FS-01–FS-03).
 */
@ApplicationScoped
@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Produk", description = "Master data produk")
public class ProductResource {

    private final ProductService productService;

    public ProductResource(ProductService productService) {
        this.productService = productService;
    }

    /** Membuat produk baru. Response sukses selalu {@code 201 Created} + header {@code Location}. */
    @POST
    @Operation(summary = "Membuat produk baru", description = "SKU harus unik dan belum pernah dipakai.")
    @APIResponse(responseCode = "201", description = "Produk berhasil dibuat")
    @APIResponse(responseCode = "400", description = "Request tidak valid (Bean Validation)")
    @APIResponse(responseCode = "409", description = "SKU sudah dipakai (DUPLICATE_SKU)")
    public RestResponse<ApiResponse<ProductResponse>> create(@Valid CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        RestResponse.ResponseBuilder<ApiResponse<ProductResponse>> response =
                RestResponse.ResponseBuilder.create(Response.Status.CREATED);
        return response
                .header("Location", URI.create("/api/v1/products/" + created.id()))
                .entity(ApiResponse.ok("Produk berhasil dibuat", created))
                .build();
    }

    /** Daftar produk dengan paging. */
    @GET
    @Operation(summary = "Daftar produk", description = "Paging berbasis page/size, maksimal 100 per halaman.")
    @APIResponse(responseCode = "200", description = "Daftar produk")
    @APIResponse(responseCode = "400", description = "Parameter paging tidak valid")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @QueryParam("page") @DefaultValue(CommonConstants.DEFAULT_PAGE)
            @Min(CommonConstants.MIN_PAGE) int page,
            @QueryParam("size") @DefaultValue(CommonConstants.DEFAULT_PAGE_SIZE)
            @Min(CommonConstants.MIN_PAGE_SIZE) @Max(CommonConstants.MAX_PAGE_SIZE) int size) {
        return ApiResponse.ok("Daftar produk", productService.list(page, size));
    }

    /** Detail satu produk. */
    @GET
    @Path("/{id}")
    @Operation(summary = "Detail produk")
    @APIResponse(responseCode = "200", description = "Detail produk")
    @APIResponse(responseCode = "404", description = "Produk tidak ditemukan (PRODUCT_NOT_FOUND)")
    public ApiResponse<ProductResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok("Detail produk", productService.get(id));
    }
}
