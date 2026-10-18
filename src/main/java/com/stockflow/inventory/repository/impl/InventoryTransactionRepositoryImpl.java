package com.stockflow.inventory.repository.impl;

import com.stockflow.inventory.constants.InventoryQueryConstants;
import com.stockflow.inventory.entity.InventoryTransaction;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementasi {@link InventoryTransactionRepository}.
 *
 * <p>Filter histori bersifat opsional (produk dan/atau gudang), sehingga kondisi JPQL disusun
 * secara dinamis dari potongan yang sudah terpusat di {@link InventoryQueryConstants}.
 */
@ApplicationScoped
public class InventoryTransactionRepositoryImpl implements InventoryTransactionRepository {

    @Override
    public Optional<InventoryTransaction> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    @Override
    public List<InventoryTransaction> page(UUID productId, UUID warehouseId, int page, int size) {
        QueryParts parts = queryParts(productId, warehouseId);
        return find(parts.jpql() + InventoryQueryConstants.ORDER_BY_CREATED_AT_DESC, parts.params())
                .page(page, size).list();
    }

    @Override
    public long total(UUID productId, UUID warehouseId) {
        QueryParts parts = queryParts(productId, warehouseId);
        return count(parts.jpql(), parts.params());
    }

    private QueryParts queryParts(UUID productId, UUID warehouseId) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (productId != null) {
            conditions.add(InventoryQueryConstants.TRANSACTION_BY_PRODUCT);
            params.put("productId", productId);
        }
        if (warehouseId != null) {
            conditions.add(InventoryQueryConstants.TRANSACTION_BY_WAREHOUSE);
            params.put("warehouseId", warehouseId);
        }
        String jpql = conditions.isEmpty()
                ? InventoryQueryConstants.NO_FILTER
                : String.join(InventoryQueryConstants.CONDITION_SEPARATOR, conditions);
        return new QueryParts(jpql, params);
    }

    private record QueryParts(String jpql, Map<String, Object> params) {
    }
}
