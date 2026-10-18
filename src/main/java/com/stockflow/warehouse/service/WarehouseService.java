package com.stockflow.warehouse.service;

import com.stockflow.common.api.PageResponse;
import com.stockflow.common.exception.BusinessRuleViolationException;
import com.stockflow.common.exception.DuplicateCodeException;
import com.stockflow.common.exception.WarehouseNotFoundException;
import com.stockflow.warehouse.api.dto.request.CreateWarehouseRequest;
import com.stockflow.warehouse.api.dto.response.WarehouseResponse;
import com.stockflow.warehouse.entity.Warehouse;
import com.stockflow.warehouse.mapper.WarehouseMapper;
import com.stockflow.warehouse.repository.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use case gudang. <b>Batas transaksi database ada di sini</b> (architecture-plan §4.3);
 * repository hanya melakukan akses data dan konversi DTO dilakukan {@link WarehouseMapper}.
 */
@ApplicationScoped
public class WarehouseService {

    private static final Logger LOG = Logger.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseRepository warehouseRepository, WarehouseMapper warehouseMapper) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
    }

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        String code = request.code().trim();
        LOG.infof("membuat gudang code=%s", code);
        if (warehouseRepository.existsByCode(code)) {
            throw new DuplicateCodeException(code);
        }

        Warehouse warehouse = warehouseRepository.create(code, request.name().trim(), request.location());
        LOG.infof("gudang dibuat id=%s code=%s", warehouse.getId(), code);
        return warehouseMapper.toResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse get(UUID id) {
        return warehouseMapper.toResponse(require(id));
    }

    @Transactional
    public PageResponse<WarehouseResponse> list(int page, int size) {
        var content = warehouseRepository.page(page, size).stream().map(warehouseMapper::toResponse).toList();
        return PageResponse.of(content, page, size, warehouseRepository.total());
    }

    @Transactional
    public Warehouse require(UUID id) {
        return warehouseRepository.findByIdOptional(id).orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    @Transactional
    public void ensureActive(UUID id) {
        Warehouse warehouse = require(id);
        if (!warehouse.isActive()) {
            throw new BusinessRuleViolationException("Gudang tidak aktif: " + id);
        }
    }
}
