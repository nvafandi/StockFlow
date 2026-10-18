package com.stockflow.product.service;

import com.stockflow.common.exception.DuplicateSkuException;
import com.stockflow.common.exception.ProductNotFoundException;
import com.stockflow.product.api.dto.request.CreateProductRequest;
import com.stockflow.product.api.dto.response.ProductResponse;
import com.stockflow.product.entity.Product;
import com.stockflow.product.mapper.ProductMapper;
import com.stockflow.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test murni (tanpa Quarkus dan tanpa Docker): repository di-mock sehingga logika use case
 * dan pemetaan DTO dapat diuji terisolasi.
 */
class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductService productService = new ProductService(productRepository, new ProductMapper());

    @Test
    @DisplayName("SKU duplikat ditolak sebelum menyentuh database")
    void skuDuplikatDitolak() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.create(new CreateProductRequest("SKU-1", "Produk", null, "PCS")));
        verify(productRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    @DisplayName("produk yang tidak ada menghasilkan PRODUCT_NOT_FOUND")
    void produkTidakAdaDitolak() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.get(id));
    }

    @Test
    @DisplayName("hasil create dikembalikan sebagai DTO (mapper), bukan entity")
    void hasilCreateBerupaDto() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id).sku("SKU-1").name("Produk").unit("PCS").active(true)
                .build();
        when(productRepository.create(any(), any(), any(), any())).thenReturn(product);

        ProductResponse response = productService.create(new CreateProductRequest("SKU-1", "Produk", null, "PCS"));

        assertEquals(id, response.id());
        assertEquals("SKU-1", response.sku());
        assertTrue(response.active());
    }
}
