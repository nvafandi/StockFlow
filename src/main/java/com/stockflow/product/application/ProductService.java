package com.stockflow.product.application;

import com.stockflow.common.exception.BusinessRuleViolationException;
import com.stockflow.common.exception.DuplicateSkuException;
import com.stockflow.common.exception.ProductNotFoundException;
import com.stockflow.common.response.PageResponse;
import com.stockflow.product.api.dto.CreateProductRequest;
import com.stockflow.product.api.dto.ProductResponse;
import com.stockflow.product.domain.Product;
import com.stockflow.product.infrastructure.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use case produk. Kelas ini <b>tidak</b> membuka transaksi; batas transaksi ada di
 * {@link ProductRepository} (lihat architecture-plan §4.1).
 */
@ApplicationScoped
public class ProductService {

    private final ProductRepository productRepository;

    @Inject
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = productRepository.create(
                sku, request.name().trim(), request.description(), request.unit().trim());
        return ProductResponse.from(product);
    }

    public ProductResponse get(UUID id) {
        return ProductResponse.from(require(id));
    }

    public PageResponse<ProductResponse> list(int page, int size) {
        var content = productRepository.page(page, size).stream().map(ProductResponse::from).toList();
        return PageResponse.of(content, page, size, productRepository.total());
    }

    public Product require(UUID id) {
        return productRepository.findByIdOptional(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Dipakai modul lain (mis. inventory) untuk memastikan produk ada dan aktif.
     * Akses lintas domain selalu lewat application service, bukan repository domain lain (AD-3).
     */
    public void ensureActive(UUID id) {
        Product product = require(id);
        if (!product.active) {
            throw new BusinessRuleViolationException("Produk tidak aktif: " + id);
        }
    }
}
