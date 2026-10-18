package com.stockflow.warehouse.repository.impl;

import com.stockflow.warehouse.entity.Warehouse;
import com.stockflow.warehouse.repository.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementasi {@link WarehouseRepository} berbasis Panache.
 */
@ApplicationScoped
public class WarehouseRepositoryImpl implements WarehouseRepository {

    @Override
    public Optional<Warehouse> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    @Override
    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    @Override
    public List<Warehouse> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    @Override
    public long total() {
        return count();
    }

    @Override
    public Warehouse create(String code, String name, String location) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Warehouse warehouse = Warehouse.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .location(location)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        persistAndFlush(warehouse);
        return warehouse;
    }
}
