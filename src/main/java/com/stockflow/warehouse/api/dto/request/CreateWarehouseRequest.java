package com.stockflow.warehouse.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body request pembuatan gudang (FSD FS-04).
 */
public record CreateWarehouseRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String name,
        String location) {
}
