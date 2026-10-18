package com.stockflow.inventory.repository.impl;

import com.stockflow.inventory.constants.InventoryQueryConstants;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.repository.InventoryMutation;
import com.stockflow.inventory.repository.InventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementasi {@link InventoryRepository}.
 *
 * <p>Mutasi stok memakai native query (SQL) karena bergantung pada fitur PostgreSQL
 * ({@code ON CONFLICT ... RETURNING}), bukan JPQL. Literal query berada di
 * {@link InventoryQueryConstants}.
 */
@ApplicationScoped
public class InventoryRepositoryImpl implements InventoryRepository {

    @Override
    public Optional<Inventory> findByProductAndWarehouse(UUID productId, UUID warehouseId) {
        return find(InventoryQueryConstants.INVENTORY_BY_PRODUCT_AND_WAREHOUSE, productId, warehouseId)
                .firstResultOptional();
    }

    @Override
    public boolean existsFor(UUID productId, UUID warehouseId) {
        return count(InventoryQueryConstants.INVENTORY_BY_PRODUCT_AND_WAREHOUSE, productId, warehouseId) > 0;
    }

    @Override
    public List<Inventory> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    @Override
    public long total() {
        return count();
    }

    @Override
    public InventoryMutation increaseQuantity(UUID productId, UUID warehouseId, long quantity) {
        Object[] row = (Object[]) getEntityManager()
                .createNativeQuery(InventoryQueryConstants.INCREASE_SQL)
                .setParameter("id", UUID.randomUUID())
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .setParameter("quantity", quantity)
                .getSingleResult();
        return toMutation(row);
    }

    @Override
    public Optional<InventoryMutation> decreaseQuantityIfEnough(UUID productId, UUID warehouseId, long quantity) {
        List<?> rows = getEntityManager()
                .createNativeQuery(InventoryQueryConstants.DECREASE_SQL)
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .setParameter("quantity", quantity)
                .getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toMutation((Object[]) rows.getFirst()));
    }

    private InventoryMutation toMutation(Object[] row) {
        return new InventoryMutation((UUID) row[0], ((Number) row[1]).longValue());
    }
}
