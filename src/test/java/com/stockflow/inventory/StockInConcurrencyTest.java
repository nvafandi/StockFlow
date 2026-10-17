package com.stockflow.inventory;

import com.stockflow.inventory.api.dto.request.StockInRequest;
import com.stockflow.inventory.service.InventoryService;
import com.stockflow.inventory.service.StockMutationOutcome;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import com.stockflow.outbox.repository.OutboxEventRepository;
import com.stockflow.support.MasterData;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bukti untuk jalur upsert (FSD §6.3): dua stock-in bersamaan atas kombinasi produk+gudang
 * yang <b>belum punya baris inventory</b>. Keduanya harus sukses, tepat satu baris inventory
 * tercipta, dan stok akhir adalah penjumlahan — bukan salah satunya menimpa yang lain.
 *
 * <p>Melengkapi {@link StockOutConcurrencyTest} yang menguji jalur {@code UPDATE} bersyarat.
 */
@QuarkusTest
class StockInConcurrencyTest {

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    InventoryTransactionRepository inventoryTransactionRepository;

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("dua stock-in bersamaan pada inventory baru tidak saling menimpa")
    void duaStockInBersamaanTidakSalingMenimpa() throws Exception {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        long transactionsBefore = inventoryTransactionRepository.count();
        long outboxBefore = outboxEventRepository.count();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger();

        Callable<Object> attempt = () -> {
            startGate.await();
            // Thread pool di luar request HTTP: aktifkan request context agar Panache
            // dapat memakai EntityManager seperti pada request sungguhan.
            ManagedContext requestContext = Arc.container().requestContext();
            requestContext.activate();
            try {
                StockMutationOutcome result = inventoryService.stockIn(UUID.randomUUID().toString(),
                        new StockInRequest(productId, warehouseId, 60, "SI-2026-0001", null));
                successCounter.incrementAndGet();
                return result;
            } catch (RuntimeException e) {
                return e;
            } finally {
                requestContext.terminate();
            }
        };

        try {
            Future<Object> first = executor.submit(attempt);
            Future<Object> second = executor.submit(attempt);
            startGate.countDown();

            List<Object> outcomes = List.of(
                    first.get(60, TimeUnit.SECONDS),
                    second.get(60, TimeUnit.SECONDS));

            assertEquals(2, successCounter.get(), () -> "kedua request harus sukses, hasil: " + outcomes);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(120L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().getQuantity(), "stok akhir harus 120 (60+60), bukan 60");
        assertEquals(1, inventoryRepository.count("productId = ?1 and warehouseId = ?2", productId, warehouseId),
                "tepat satu baris inventory untuk kombinasi produk+gudang");
        assertEquals(transactionsBefore + 2, inventoryTransactionRepository.count());
        assertEquals(outboxBefore + 2, outboxEventRepository.count());
    }
}
