package com.stockflow.product.infrastructure;

import com.stockflow.product.domain.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository produk (Panache).
 *
 * <p>Batas transaksi database berada di repository: setiap operasi tulis dianotasi
 * {@code @Transactional} dan melakukan {@code flush} di dalam boundary sehingga
 * pelanggaran constraint dapat diterjemahkan menjadi exception domain di tempat.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Optional<Product> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public Optional<Product> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    public boolean existsBySku(String sku) {
        return count("sku", sku) > 0;
    }

    public List<Product> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    public long total() {
        return count();
    }

    @Transactional
    public Product create(String sku, String name, String description, String unit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Product product = new Product();
        product.id = UUID.randomUUID();
        product.sku = sku;
        product.name = name;
        product.description = description;
        product.unit = unit;
        product.active = true;
        product.createdAt = now;
        product.updatedAt = now;

        persistAndFlush(product);
        return product;
    }

    @Transactional
    public Product updateStatus(UUID id, boolean active) {
        Product product = findByIdOptional(id)
                .orElseThrow(() -> new IllegalStateException("Product harus sudah tervalidasi sebelum update: " + id));
        product.active = active;
        product.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        flush();
        return product;
    }
}
