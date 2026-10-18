package com.stockflow.warehouse.mapper;

import com.stockflow.warehouse.api.dto.response.WarehouseResponse;
import com.stockflow.warehouse.entity.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Konversi entity ↔ DTO gudang (satu-satunya tempat pemetaan, AD-5/AD-6).
 */
@ApplicationScoped
public class WarehouseMapper {

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.isActive());
    }
}
