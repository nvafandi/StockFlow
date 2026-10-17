package com.stockflow.inventory;

import com.stockflow.inventory.domain.StockMutationCommand;
import com.stockflow.inventory.infrastructure.InventoryRepository;
import com.stockflow.inventory.infrastructure.InventoryTransactionRepository;
import com.stockflow.inventory.infrastructure.StockMutationRepository;
import com.stockflow.outbox.infrastructure.OutboxEventRepository;
import com.stockflow.support.MasterData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Membuktikan atomisitas transaksi yang dibuka repository: kegagalan pada penulisan
 * histori (langkah setelah perubahan stok) harus membatalkan perubahan stok sekaligus
 * membatalkan penulisan event outbox.
 */
@QuarkusTest
class StockMutationAtomicityTest {

    @Inject
    StockMutationRepository stockMutationRepository;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    InventoryTransactionRepository inventoryTransactionRepository;

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("kegagalan di tengah transaksi membatalkan stok, histori, dan outbox sekaligus")
    void kegagalanDiTengahTransaksiMembatalkanSeluruhPerubahan() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        stockMutationRepository.applyStockIn(StockMutationCommand.of(
                productId, warehouseId, 10, "SEED", UUID.randomUUID().toString(), null));

        assertEquals(10L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().quantity);

        long transactionsBefore = inventoryTransactionRepository.count();
        long outboxBefore = outboxEventRepository.count();

        // reference_id melebihi VARCHAR(100) → gagal setelah UPDATE inventory berhasil.
        StockMutationCommand invalid = StockMutationCommand.of(
                productId, warehouseId, 50, "X".repeat(150), UUID.randomUUID().toString(), null);

        assertThrows(RuntimeException.class, () -> stockMutationRepository.applyStockIn(invalid));

        assertEquals(10L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().quantity, "perubahan stok harus di-rollback");
        assertEquals(transactionsBefore, inventoryTransactionRepository.count(), "histori tidak boleh bertambah");
        assertEquals(outboxBefore, outboxEventRepository.count(), "event outbox tidak boleh bertambah");
    }
}
