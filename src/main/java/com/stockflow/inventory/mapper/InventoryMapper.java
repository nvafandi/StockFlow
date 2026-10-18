package com.stockflow.inventory.mapper;

import com.stockflow.inventory.api.dto.response.InventoryResponse;
import com.stockflow.inventory.api.dto.response.InventoryTransactionResponse;
import com.stockflow.inventory.api.dto.response.StockMutationResponse;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.entity.InventoryTransaction;
import com.stockflow.inventory.service.StockMutationResult;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Konversi entity/value object ↔ DTO inventory (satu-satunya tempat pemetaan, AD-5/AD-6).
 *
 * <p>Overload {@link #toResponse(StockMutationResult)} menerima value object application layer
 * (hasil satu use case), bukan entity — tetap berada di sini agar service dan API layer tidak
 * memuat logika konversi.
 */
@ApplicationScoped
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getWarehouseId(),
                inventory.getQuantity(),
                inventory.getUpdatedAt());
    }

    public InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType().name(),
                transaction.getQuantityDelta(),
                transaction.getQuantityBefore(),
                transaction.getQuantityAfter(),
                transaction.getReferenceId(),
                transaction.getCreatedAt());
    }

    public StockMutationResponse toResponse(StockMutationResult result) {
        return new StockMutationResponse(
                result.productId(),
                result.warehouseId(),
                result.quantityBefore(),
                result.quantityChanged(),
                result.quantityAfter(),
                result.transactionId());
    }
}
