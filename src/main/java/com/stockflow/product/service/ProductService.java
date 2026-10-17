package com.stockflow.product.service;

import com.stockflow.common.api.PageResponse;
import com.stockflow.common.exception.BusinessRuleViolationException;
import com.stockflow.common.exception.DuplicateSkuException;
import com.stockflow.common.exception.ProductNotFoundException;
import com.stockflow.product.api.dto.request.CreateProductRequest;
import com.stockflow.product.api.dto.response.ProductResponse;
import com.stockflow.product.entity.Product;
import com.stockflow.product.mapper.ProductMapper;
import com.stockflow.product.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use case produk.
 *
 * <p><b>Batas transaksi database ada di sini</b> (architecture-plan §4.3): operasi tulis maupun
 * baca dijalankan dalam transaksi ({@code jakarta.transaction.Transactional} tidak menyediakan
 * atribut {@code readOnly}, berbeda dari {@code @Transactional} milik Spring). Repository hanya
 * melakukan akses data, sedangkan konversi DTO dilakukan {@link ProductMapper}.
 */
@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim();
        LOG.infof("membuat produk sku=%s", sku);
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = productRepository.create(
                sku, request.name().trim(), request.description(), request.unit().trim());
        LOG.infof("produk dibuat id=%s sku=%s", product.getId(), sku);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse get(UUID id) {
        return productMapper.toResponse(require(id));
    }

    @Transactional
    public PageResponse<ProductResponse> list(int page, int size) {
        var content = productRepository.page(page, size).stream().map(productMapper::toResponse).toList();
        return PageResponse.of(content, page, size, productRepository.total());
    }

    @Transactional
    public Product require(UUID id) {
        return productRepository.findByIdOptional(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Dipakai modul lain (mis. inventory) untuk memastikan produk ada dan aktif.
     * Akses lintas domain selalu lewat application service, bukan repository domain lain (AD-3).
     */
    @Transactional
    public void ensureActive(UUID id) {
        Product product = require(id);
        if (!product.isActive()) {
            throw new BusinessRuleViolationException("Produk tidak aktif: " + id);
        }
    }
}
