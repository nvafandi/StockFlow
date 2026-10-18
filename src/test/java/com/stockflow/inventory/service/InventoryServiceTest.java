package com.stockflow.inventory.service;

import com.stockflow.common.exception.BusinessException;
import com.stockflow.common.exception.ConcurrencyConflictException;
import com.stockflow.common.exception.ErrorCode;
import com.stockflow.inventory.api.dto.request.StockInRequest;
import com.stockflow.inventory.entity.InventoryTransaction;
import com.stockflow.inventory.entity.TransactionType;
import com.stockflow.inventory.mapper.InventoryMapper;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import com.stockflow.product.service.ProductService;
import com.stockflow.warehouse.service.WarehouseService;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test orkestrator inventory (tanpa Quarkus dan tanpa Docker).
 *
 * <p>Fokus: urutan validasi sebelum transaksi dan pemulihan balapan {@code Idempotency-Key}
 * (R1) — jalur yang di produksi hanya muncul saat dua request kembar tiba bersamaan.
 */
class InventoryServiceTest {

    private static final String EXISTING_KEY = "11111111-1111-1111-1111-111111111111";

    private final ProductService productService = mock(ProductService.class);
    private final WarehouseService warehouseService = mock(WarehouseService.class);
    private final InventoryMutationService mutationService = mock(InventoryMutationService.class);
    private final InventoryRepository inventoryRepository = mock(InventoryRepository.class);
    private final InventoryTransactionRepository transactionRepository = mock(InventoryTransactionRepository.class);
    private final InventoryService inventoryService = new InventoryService(productService, warehouseService,
            mutationService, inventoryRepository, transactionRepository, new InventoryMapper());

    private final UUID productId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();

    @Test
    @DisplayName("header Idempotency-Key kosong ditolak sebelum validasi referensi dan transaksi")
    void idempotencyKeyKosongDitolak() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.stockIn("   ", stockIn()));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.errorCode());
        verifyNoInteractions(mutationService);
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("Idempotency-Key melebihi 150 karakter ditolak")
    void idempotencyKeyTerlaluPanjangDitolak() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> inventoryService.stockIn("x".repeat(151), stockIn()));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.errorCode());
        verifyNoInteractions(mutationService);
    }

    @Test
    @DisplayName("mutasi baru tidak ditandai reused (API memetakannya ke 201 Created)")
    void mutasiBaruTidakReused() {
        when(mutationService.applyStockIn(any())).thenReturn(StockMutationResult.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                productId, warehouseId, 0, 5, 5));

        StockMutationOutcome outcome = inventoryService.stockIn(EXISTING_KEY, stockIn());

        assertFalse(outcome.reused());
        assertEquals(5, outcome.response().quantityAfter());
        assertEquals(productId, outcome.response().productId());
    }

    @Test
    @DisplayName("balapan Idempotency-Key dipulihkan di luar transaksi yang gagal (R1)")
    void balapanIdempotencyKeyDipulihkan() {
        when(mutationService.applyStockIn(any())).thenThrow(uniqueViolationOnIdempotencyKey());
        when(mutationService.findReused(any())).thenReturn(Optional.of(reusedResult()));

        StockMutationOutcome outcome = inventoryService.stockIn(EXISTING_KEY, stockIn());

        assertTrue(outcome.reused());
        assertEquals(5, outcome.response().quantityBefore());
        assertEquals(10, outcome.response().quantityAfter());
        verify(mutationService).findReused(any());
    }

    @Test
    @DisplayName("balapan tanpa transaksi pemenang menghasilkan CONCURRENCY_CONFLICT")
    void balapanTanpaPemenangDitolak() {
        when(mutationService.applyStockIn(any())).thenThrow(uniqueViolationOnIdempotencyKey());
        when(mutationService.findReused(any())).thenReturn(Optional.empty());

        assertThrows(ConcurrencyConflictException.class, () -> inventoryService.stockIn(EXISTING_KEY, stockIn()));
    }

    @Test
    @DisplayName("kegagalan yang bukan pelanggaran Idempotency-Key diteruskan apa adanya")
    void kegagalanLainDiteruskan() {
        when(mutationService.applyStockIn(any())).thenThrow(new IllegalStateException("bukan pelanggaran unik"));

        assertThrows(IllegalStateException.class, () -> inventoryService.stockIn(EXISTING_KEY, stockIn()));
    }

    private StockInRequest stockIn() {
        return new StockInRequest(productId, warehouseId, 5, "REF-1", null);
    }

    private PersistenceException uniqueViolationOnIdempotencyKey() {
        return new PersistenceException(new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key"), "uk_inventory_transactions_idempotency_key"));
    }

    private StockMutationResult reusedResult() {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .warehouseId(warehouseId)
                .transactionType(TransactionType.IN)
                .quantityDelta(5)
                .quantityBefore(5)
                .quantityAfter(10)
                .build();
        return StockMutationResult.reused(transaction);
    }
}
