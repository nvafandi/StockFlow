package com.stockflow.product.repository.impl;

import com.stockflow.product.entity.Product;
import com.stockflow.product.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementasi {@link ProductRepository} berbasis Panache.
 *
 * <p>Tanpa anotasi {@code @Transactional}: seluruh operasi berpartisipasi pada transaksi
 * milik application service (architecture-plan §4.3).
 */
@ApplicationScoped
public class ProductRepositoryImpl implements ProductRepository {

    @Override
    public Optional<Product> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    @Override
    public boolean existsBySku(String sku) {
        return count("sku", sku) > 0;
    }

    @Override
    public List<Product> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    @Override
    public long total() {
        return count();
    }

    @Override
    public Product create(String sku, String name, String description, String unit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .sku(sku)
                .name(name)
                .description(description)
                .unit(unit)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        persistAndFlush(product);
        return product;
    }

    @Override
    public Product updateStatus(UUID id, boolean active) {
        Product product = findByIdOptional(id)
                .orElseThrow(() -> new IllegalStateException("Product harus sudah tervalidasi sebelum update: " + id));
        product.setActive(active);
        product.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        flush();
        return product;
    }
}
