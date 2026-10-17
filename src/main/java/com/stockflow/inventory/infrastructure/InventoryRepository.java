package com.stockflow.inventory.infrastructure;

import com.stockflow.inventory.domain.Inventory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Akses data {@code inventory}.
 *
 * <p>Dua method mutasi di bawah sengaja <b>tidak</b> dianotasi {@code @Transactional}:
 * keduanya harus berjalan di dalam transaksi use case yang sama
 * ({@code StockMutationRepository}), bukan membuka transaksi sendiri.
 *
 * <p>Keduanya memakai satu statement SQL atomik sehingga aman terhadap request bersamaan:
 *
 * <ul>
 *   <li>stock in : {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}
 *   <li>stock out: {@code UPDATE ... WHERE quantity >= :qty RETURNING} (0 baris = stok tidak cukup)
 * </ul>
 */
@ApplicationScoped
public class InventoryRepository implements PanacheRepository<Inventory> {

    private static final String INCREASE_SQL = """
            INSERT INTO inventory (id, product_id, warehouse_id, quantity, version, updated_at)
            VALUES (:id, :productId, :warehouseId, :quantity, 1, now())
            ON CONFLICT (product_id, warehouse_id)
            DO UPDATE SET quantity   = inventory.quantity + EXCLUDED.quantity,
                          version    = inventory.version + 1,
                          updated_at = now()
            RETURNING id, quantity
            """;

    private static final String DECREASE_SQL = """
            UPDATE inventory
               SET quantity   = quantity - :quantity,
                   version    = version + 1,
                   updated_at = now()
             WHERE product_id = :productId
               AND warehouse_id = :warehouseId
               AND quantity >= :quantity
            RETURNING id, quantity
            """;

    public Optional<Inventory> findByProductAndWarehouse(UUID productId, UUID warehouseId) {
        return find("productId = ?1 and warehouseId = ?2", productId, warehouseId).firstResultOptional();
    }

    public boolean existsFor(UUID productId, UUID warehouseId) {
        return count("productId = ?1 and warehouseId = ?2", productId, warehouseId) > 0;
    }

    public List<Inventory> page(int page, int size) {
        return findAll().page(page, size).list();
    }

    public long total() {
        return count();
    }

    InventoryMutation increaseQuantity(UUID productId, UUID warehouseId, long quantity) {
        Object[] row = (Object[]) getEntityManager()
                .createNativeQuery(INCREASE_SQL)
                .setParameter("id", UUID.randomUUID())
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .setParameter("quantity", quantity)
                .getSingleResult();
        return toMutation(row);
    }

    Optional<InventoryMutation> decreaseQuantityIfEnough(UUID productId, UUID warehouseId, long quantity) {
        List<?> rows = getEntityManager()
                .createNativeQuery(DECREASE_SQL)
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
