package com.stockflow.inventory.infrastructure;

import com.stockflow.inventory.domain.InventoryTransaction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Akses data histori transaksi stok (append-only).
 */
@ApplicationScoped
public class InventoryTransactionRepository implements PanacheRepository<InventoryTransaction> {

    public Optional<InventoryTransaction> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    public List<InventoryTransaction> page(UUID productId, UUID warehouseId, int page, int size) {
        QueryParts parts = queryParts(productId, warehouseId);
        return find(parts.jpql() + " order by createdAt desc", parts.params()).page(page, size).list();
    }

    public long total(UUID productId, UUID warehouseId) {
        QueryParts parts = queryParts(productId, warehouseId);
        return count(parts.jpql(), parts.params());
    }

    private QueryParts queryParts(UUID productId, UUID warehouseId) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (productId != null) {
            conditions.add("productId = :productId");
            params.put("productId", productId);
        }
        if (warehouseId != null) {
            conditions.add("warehouseId = :warehouseId");
            params.put("warehouseId", warehouseId);
        }
        String jpql = conditions.isEmpty() ? "1 = 1" : String.join(" and ", conditions);
        return new QueryParts(jpql, params);
    }

    private record QueryParts(String jpql, Map<String, Object> params) {
    }
}
