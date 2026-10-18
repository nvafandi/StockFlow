package com.stockflow.product.mapper;

import com.stockflow.product.api.dto.response.ProductResponse;
import com.stockflow.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Konversi entity ↔ DTO produk.
 *
 * <p>Satu-satunya tempat pemetaan domain produk (AD-5/AD-6): DTO tidak boleh memuat logika
 * konversi dan entity tidak boleh keluar dari batas modul.
 */
@ApplicationScoped
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnit(),
                product.isActive());
    }
}
