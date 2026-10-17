package com.stockflow.inventory.api.dto;

import com.stockflow.inventory.domain.StockMutationResult;

import java.util.UUID;

/**
 * Bentuk response FS-07 / FS-08.
 */
public record StockMutationResponse(
        UUID productId,
        UUID warehouseId,
        long quantityBefore,
        long quantityChanged,
        long quantityAfter,
        UUID transactionId) {

    public static StockMutationResponse from(StockMutationResult result) {
        return new StockMutationResponse(
                result.productId(),
                result.warehouseId(),
                result.quantityBefore(),
                result.quantityChanged(),
                result.quantityAfter(),
                result.transactionId());
    }
}
