package com.stockflow.warehouse.application;

import com.stockflow.common.exception.BusinessRuleViolationException;
import com.stockflow.common.exception.DuplicateCodeException;
import com.stockflow.common.exception.WarehouseNotFoundException;
import com.stockflow.common.response.PageResponse;
import com.stockflow.warehouse.api.dto.CreateWarehouseRequest;
import com.stockflow.warehouse.api.dto.WarehouseResponse;
import com.stockflow.warehouse.domain.Warehouse;
import com.stockflow.warehouse.infrastructure.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use case gudang. Tidak membuka transaksi; batas transaksi ada di repository.
 */
@ApplicationScoped
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Inject
    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public WarehouseResponse create(CreateWarehouseRequest request) {
        String code = request.code().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw new DuplicateCodeException(code);
        }

        Warehouse warehouse = warehouseRepository.create(code, request.name().trim(), request.location());
        return WarehouseResponse.from(warehouse);
    }

    public WarehouseResponse get(UUID id) {
        return WarehouseResponse.from(require(id));
    }

    public PageResponse<WarehouseResponse> list(int page, int size) {
        var content = warehouseRepository.page(page, size).stream().map(WarehouseResponse::from).toList();
        return PageResponse.of(content, page, size, warehouseRepository.total());
    }

    public Warehouse require(UUID id) {
        return warehouseRepository.findByIdOptional(id).orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    public void ensureActive(UUID id) {
        Warehouse warehouse = require(id);
        if (!warehouse.active) {
            throw new BusinessRuleViolationException("Gudang tidak aktif: " + id);
        }
    }
}
