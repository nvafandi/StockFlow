package com.stockflow.warehouse.api.dto.response;

import java.util.UUID;

/**
 * Representasi gudang pada API. Konversi dari entity dilakukan {@code WarehouseMapper}.
 */
public record WarehouseResponse(UUID id, String code, String name, String location, boolean active) {
}
