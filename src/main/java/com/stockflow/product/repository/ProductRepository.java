package com.stockflow.product.repository;

import com.stockflow.product.entity.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kontrak akses data produk.
 *
 * <p>Service bergantung pada interface ini, bukan pada implementasinya (Dependency Inversion,
 * AD-7). Repository hanya berisi operasi data: tanpa anotasi {@code @Transactional} dan tanpa
 * logika bisnis — batas transaksi database berada di application service
 * ({@code ProductService}), yang memanggil {@code flush} di dalam transaksinya sehingga
 * pelanggaran constraint tetap dapat diterjemahkan menjadi exception domain
 * (lihat architecture-plan §4.3).
 */
public interface ProductRepository extends PanacheRepository<Product> {

    Optional<Product> findByIdOptional(UUID id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> page(int page, int size);

    long total();

    /**
     * Menyimpan produk baru dan langsung {@code flush}, agar pelanggaran unique {@code sku}
     * terdeteksi di dalam transaksi pemanggil.
     */
    Product create(String sku, String name, String description, String unit);

    /**
     * Mengubah status aktif produk. Dipakai untuk aktivasi/deaktivasi; pemanggil wajib berada
     * di dalam transaksi.
     */
    Product updateStatus(UUID id, boolean active);
}
