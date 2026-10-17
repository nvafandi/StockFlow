package com.stockflow.inventory.api.dto;

import com.stockflow.inventory.domain.InventoryTransaction;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryTransactionResponse(
        UUID transactionId,
        String transactionType,
        long quantityDelta,
        long quantityBefore,
        long quantityAfter,
        String referenceId,
        OffsetDateTime createdAt) {

    public static InventoryTransactionResponse from(InventoryTransaction transaction) {
        return new InventoryTransactionResponse(
                transaction.id,
                transaction.transactionType.name(),
                transaction.quantityDelta,
                transaction.quantityBefore,
                transaction.quantityAfter,
                transaction.referenceId,
                transaction.createdAt);
    }
}
