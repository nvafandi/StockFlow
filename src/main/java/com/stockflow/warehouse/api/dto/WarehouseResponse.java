package com.stockflow.warehouse.api.dto;

import com.stockflow.warehouse.domain.Warehouse;

import java.util.UUID;

public record WarehouseResponse(UUID id, String code, String name, String location, boolean active) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.id, warehouse.code, warehouse.name, warehouse.location, warehouse.active);
    }
}
