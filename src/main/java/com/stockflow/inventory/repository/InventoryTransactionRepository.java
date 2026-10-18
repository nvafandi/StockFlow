package com.stockflow.inventory.repository;

import com.stockflow.inventory.entity.InventoryTransaction;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kontrak akses data histori transaksi stok (append-only).
 *
 * <p>Tanpa anotasi {@code @Transactional}: penulisan menjadi bagian dari transaksi use case
 * milik {@code InventoryMutationService}.
 */
public interface InventoryTransactionRepository extends PanacheRepository<InventoryTransaction> {

    Optional<InventoryTransaction> findByIdempotencyKey(String idempotencyKey);

    List<InventoryTransaction> page(UUID productId, UUID warehouseId, int page, int size);

    long total(UUID productId, UUID warehouseId);
}
