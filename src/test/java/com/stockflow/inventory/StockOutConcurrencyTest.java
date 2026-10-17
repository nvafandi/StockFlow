package com.stockflow.inventory;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.inventory.api.dto.StockInRequest;
import com.stockflow.inventory.api.dto.StockOutRequest;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.inventory.domain.StockMutationResult;
import com.stockflow.inventory.infrastructure.InventoryRepository;
import com.stockflow.inventory.infrastructure.InventoryTransactionRepository;
import com.stockflow.outbox.infrastructure.OutboxEventRepository;
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
 * Bukti yang diminta FSD §6.3: dua stock-out bersamaan atas stok 100 dengan qty 60
 * (a) menyisakan tepat 40, (b) hanya satu request sukses, (c) hanya satu histori dan
 * satu event outbox yang tercipta.
 */
@QuarkusTest
class StockOutConcurrencyTest {

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    InventoryTransactionRepository inventoryTransactionRepository;

    @Inject
    OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("dua stock-out bersamaan tidak menyebabkan overselling")
    void duaStockOutBersamaanTidakOversell() throws Exception {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        inventoryService.stockIn(UUID.randomUUID().toString(),
                new StockInRequest(productId, warehouseId, 100, "SEED", null));

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
                StockMutationResult result = inventoryService.stockOut(UUID.randomUUID().toString(),
                        new StockOutRequest(productId, warehouseId, 60, "SO-2026-0001", null));
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

            assertEquals(1, successCounter.get(), () -> "tepat satu request harus sukses, hasil: " + outcomes);
            outcomes.stream()
                    .filter(InsufficientStockException.class::isInstance)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("satu request harus gagal dengan 409, hasil: " + outcomes));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(40L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().quantity, "stok akhir harus 40, bukan -20");
        assertEquals(transactionsBefore + 1, inventoryTransactionRepository.count());
        assertEquals(outboxBefore + 1, outboxEventRepository.count());
    }
}
