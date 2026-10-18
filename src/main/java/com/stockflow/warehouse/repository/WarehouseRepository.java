package com.stockflow.warehouse.repository;

import com.stockflow.warehouse.entity.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kontrak akses data gudang (Dependency Inversion, AD-7).
 *
 * <p>Tanpa anotasi {@code @Transactional} dan tanpa logika bisnis: batas transaksi database
 * berada di application service ({@code WarehouseService}).
 */
public interface WarehouseRepository extends PanacheRepository<Warehouse> {

    Optional<Warehouse> findByIdOptional(UUID id);

    boolean existsByCode(String code);

    List<Warehouse> page(int page, int size);

    long total();

    /**
     * Menyimpan gudang baru dan langsung {@code flush}, agar pelanggaran unique {@code code}
     * terdeteksi di dalam transaksi pemanggil.
     */
    Warehouse create(String code, String name, String location);
}
