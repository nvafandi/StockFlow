package com.stockflow.product.api;

import com.stockflow.common.response.ApiResponse;
import com.stockflow.common.response.PageResponse;
import com.stockflow.product.api.dto.CreateProductRequest;
import com.stockflow.product.api.dto.ProductResponse;
import com.stockflow.product.application.ProductService;
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

@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductService productService;

    @Inject
    public ProductResource(ProductService productService) {
        this.productService = productService;
    }

    @POST
    public Response create(@Valid CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        return Response.created(URI.create("/api/v1/products/" + created.id()))
                .entity(ApiResponse.ok("Produk berhasil dibuat", created))
                .build();
    }

    @GET
    public ApiResponse<PageResponse<ProductResponse>> list(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Daftar produk", productService.list(page, size));
    }

    @GET
    @Path("/{id}")
    public ApiResponse<ProductResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok("Detail produk", productService.get(id));
    }
}
