package com.stockflow.inventory.application;

import com.stockflow.common.exception.BusinessException;
import com.stockflow.common.exception.ConcurrencyConflictException;
import com.stockflow.common.exception.InventoryNotFoundException;
import com.stockflow.common.response.ErrorCode;
import com.stockflow.common.response.PageResponse;
import com.stockflow.inventory.api.dto.InventoryResponse;
import com.stockflow.inventory.api.dto.InventoryTransactionResponse;
import com.stockflow.inventory.api.dto.StockInRequest;
import com.stockflow.inventory.api.dto.StockOutRequest;
import com.stockflow.inventory.domain.StockMutationCommand;
import com.stockflow.inventory.domain.StockMutationResult;
import com.stockflow.inventory.infrastructure.InventoryRepository;
import com.stockflow.inventory.infrastructure.InventoryTransactionRepository;
import com.stockflow.inventory.infrastructure.StockMutationRepository;
import com.stockflow.product.application.ProductService;
import com.stockflow.warehouse.application.WarehouseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.function.Function;

/**
 * Use case mutasi stok.
 *
 * <p>Kelas ini <b>tidak</b> membuka transaksi database: batas transaksi berada di
 * {@link StockMutationRepository} (satu method = satu transaksi). Tugas service hanya:
 *
 * <ol>
 *   <li>memvalidasi header dan referensi lintas domain (via application service domain lain,
 *       sesuai AD-3);
 *   <li>mendelegasikan mutasi ke repository;
 *   <li>menangani balapan {@code Idempotency-Key} yang lolos dari pre-check.
 * </ol>
 */
@ApplicationScoped
public class InventoryService {

    private static final Logger LOG = Logger.getLogger(InventoryService.class);
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 150;

    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final StockMutationRepository stockMutationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Inject
    public InventoryService(ProductService productService,
                            WarehouseService warehouseService,
                            StockMutationRepository stockMutationRepository,
                            InventoryRepository inventoryRepository,
                            InventoryTransactionRepository inventoryTransactionRepository) {
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.stockMutationRepository = stockMutationRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    public StockMutationResult stockIn(String idempotencyKey, StockInRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        requireActiveReferences(request.productId(), request.warehouseId());

        StockMutationCommand command = StockMutationCommand.of(request.productId(), request.warehouseId(),
                request.quantity(), request.referenceId(), key, request.notes());

        return execute(command, stockMutationRepository::applyStockIn);
    }

    public StockMutationResult stockOut(String idempotencyKey, StockOutRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        requireActiveReferences(request.productId(), request.warehouseId());

        StockMutationCommand command = StockMutationCommand.of(request.productId(), request.warehouseId(),
                request.quantity(), request.referenceId(), key, request.notes());

        return execute(command, stockMutationRepository::applyStockOut);
    }

    public InventoryResponse getStock(UUID productId, UUID warehouseId) {
        return inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new InventoryNotFoundException(productId, warehouseId));
    }

    public PageResponse<InventoryResponse> list(int page, int size) {
        var content = inventoryRepository.page(page, size).stream().map(InventoryResponse::from).toList();
        return PageResponse.of(content, page, size, inventoryRepository.total());
    }

    public PageResponse<InventoryTransactionResponse> history(UUID productId, UUID warehouseId, int page, int size) {
        var content = inventoryTransactionRepository.page(productId, warehouseId, page, size).stream()
                .map(InventoryTransactionResponse::from)
                .toList();
        return PageResponse.of(content, page, size, inventoryTransactionRepository.total(productId, warehouseId));
    }

    private void requireActiveReferences(UUID productId, UUID warehouseId) {
        productService.ensureActive(productId);
        warehouseService.ensureActive(warehouseId);
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Header Idempotency-Key wajib dikirim");
        }
        String key = idempotencyKey.trim();
        if (key.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key maksimal %d karakter".formatted(IDEMPOTENCY_KEY_MAX_LENGTH));
        }
        return key;
    }

    /**
     * Menjalankan mutasi pada transaksi milik repository.
     *
     * <p>Bila dua request dengan {@code Idempotency-Key} sama tiba bersamaan, keduanya lolos
     * pre-check; salah satunya gagal pada unique constraint dan transaksinya di-rollback.
     * Yang kalah kemudian membaca transaksi pemenang dan mengembalikan hasil yang sama
     * (FSD §6.1 baris 4).
     */
    private StockMutationResult execute(StockMutationCommand command,
                                        Function<StockMutationCommand, StockMutationResult> mutation) {
        try {
            return mutation.apply(command);
        } catch (PersistenceException e) {
            if (!isIdempotencyKeyViolation(e)) {
                throw e;
            }
            LOG.infof("Idempotency-Key %s kalah balapan; mengembalikan hasil transaksi pemenang",
                    command.idempotencyKey());
            return stockMutationRepository.findReused(command)
                    .orElseThrow(() -> new ConcurrencyConflictException(command.idempotencyKey()));
        }
    }

    private boolean isIdempotencyKeyViolation(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                return constraint == null || constraint.contains("idempotency_key");
            }
        }
        return false;
    }
}
