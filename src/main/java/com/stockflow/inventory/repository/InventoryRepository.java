package com.stockflow.inventory.repository;

import com.stockflow.inventory.entity.Inventory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kontrak akses data {@code inventory}.
 *
 * <p>Dua method mutasi di bawah sengaja <b>tidak</b> dianotasi {@code @Transactional}:
 * keduanya harus berjalan di dalam transaksi use case yang sama
 * ({@code InventoryMutationService}), bukan membuka transaksi sendiri.
 *
 * <p>Keduanya memakai satu statement SQL atomik sehingga aman terhadap request bersamaan:
 *
 * <ul>
 *   <li>stock in : {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}
 *   <li>stock out: {@code UPDATE ... WHERE quantity >= :qty RETURNING} (0 baris = stok tidak cukup)
 * </ul>
 */
public interface InventoryRepository extends PanacheRepository<Inventory> {

    Optional<Inventory> findByProductAndWarehouse(UUID productId, UUID warehouseId);

    boolean existsFor(UUID productId, UUID warehouseId);

    List<Inventory> page(int page, int size);

    long total();

    /**
     * Menambah stok dalam satu statement atomik (upsert) dan mengembalikan
     * {@code inventoryId} beserta kuantitas setelah mutasi.
     */
    InventoryMutation increaseQuantity(UUID productId, UUID warehouseId, long quantity);

    /**
     * Mengurangi stok bila mencukupi. {@link Optional#empty()} berarti tidak ada baris yang
     * memenuhi predikat {@code quantity >= :quantity}.
     */
    Optional<InventoryMutation> decreaseQuantityIfEnough(UUID productId, UUID warehouseId, long quantity);
}
