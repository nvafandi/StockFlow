package com.stockflow.warehouse.infrastructure;

import com.stockflow.warehouse.domain.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository gudang. Batas transaksi database ada di sini (lihat architecture-plan §4.1).
 */
@ApplicationScoped
public class WarehouseRepository implements PanacheRepository<Warehouse> {

    public Optional<Warehouse> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    public List<Warehouse> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    public long total() {
        return count();
    }

    @Transactional
    public Warehouse create(String code, String name, String location) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Warehouse warehouse = new Warehouse();
        warehouse.id = UUID.randomUUID();
        warehouse.code = code;
        warehouse.name = name;
        warehouse.location = location;
        warehouse.active = true;
        warehouse.createdAt = now;
        warehouse.updatedAt = now;

        persistAndFlush(warehouse);
        return warehouse;
    }
}
