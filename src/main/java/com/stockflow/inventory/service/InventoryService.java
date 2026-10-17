package com.stockflow.inventory.service;

import com.stockflow.common.api.PageResponse;
import com.stockflow.common.exception.BusinessException;
import com.stockflow.common.exception.ConcurrencyConflictException;
import com.stockflow.common.exception.ErrorCode;
import com.stockflow.common.exception.InventoryNotFoundException;
import com.stockflow.inventory.api.dto.request.StockInRequest;
import com.stockflow.inventory.api.dto.request.StockOutRequest;
import com.stockflow.inventory.api.dto.response.InventoryResponse;
import com.stockflow.inventory.api.dto.response.InventoryTransactionResponse;
import com.stockflow.inventory.constants.InventoryConstants;
import com.stockflow.inventory.mapper.InventoryMapper;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import com.stockflow.product.service.ProductService;
import com.stockflow.warehouse.service.WarehouseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.function.Function;

/**
 * Orkestrator use case mutasi stok.
 *
 * <p>Kelas ini <b>tidak</b> membuka transaksi database. Batas transaksi berada di
 * {@link InventoryMutationService} (satu method = satu transaksi). Tugas orkestrator:
 *
 * <ol>
 *   <li>memvalidasi header dan referensi lintas domain (via application service domain lain,
 *       sesuai AD-3);
 *   <li>mendelegasikan mutasi ke {@link InventoryMutationService};
 *   <li>menangani balapan {@code Idempotency-Key} yang lolos dari pre-check — penanganan ini
 *       harus berada di luar transaksi, karena transaksi tempat {@code flush()} gagal sudah
 *       <i>rollback-only</i>.
 * </ol>
 */
@ApplicationScoped
public class InventoryService {

    private static final Logger LOG = Logger.getLogger(InventoryService.class);

    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final InventoryMutationService inventoryMutationService;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(ProductService productService,
                            WarehouseService warehouseService,
                            InventoryMutationService inventoryMutationService,
                            InventoryRepository inventoryRepository,
                            InventoryTransactionRepository inventoryTransactionRepository,
                            InventoryMapper inventoryMapper) {
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.inventoryMutationService = inventoryMutationService;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryMapper = inventoryMapper;
    }

    public StockMutationOutcome stockIn(String idempotencyKey, StockInRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        requireActiveReferences(request.productId(), request.warehouseId());

        StockMutationCommand command = StockMutationCommand.of(request.productId(), request.warehouseId(),
                request.quantity(), request.referenceId(), key, request.notes());

        return outcome(execute(command, inventoryMutationService::applyStockIn));
    }

    public StockMutationOutcome stockOut(String idempotencyKey, StockOutRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        requireActiveReferences(request.productId(), request.warehouseId());

        StockMutationCommand command = StockMutationCommand.of(request.productId(), request.warehouseId(),
                request.quantity(), request.referenceId(), key, request.notes());

        return outcome(execute(command, inventoryMutationService::applyStockOut));
    }

    public InventoryResponse getStock(UUID productId, UUID warehouseId) {
        return inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException(productId, warehouseId));
    }

    public PageResponse<InventoryResponse> list(int page, int size) {
        var content = inventoryRepository.page(page, size).stream().map(inventoryMapper::toResponse).toList();
        return PageResponse.of(content, page, size, inventoryRepository.total());
    }

    public PageResponse<InventoryTransactionResponse> history(UUID productId, UUID warehouseId, int page, int size) {
        var content = inventoryTransactionRepository.page(productId, warehouseId, page, size).stream()
                .map(inventoryMapper::toResponse)
                .toList();
        return PageResponse.of(content, page, size, inventoryTransactionRepository.total(productId, warehouseId));
    }

    private StockMutationOutcome outcome(StockMutationResult result) {
        return new StockMutationOutcome(inventoryMapper.toResponse(result), result.reused());
    }

    private void requireActiveReferences(UUID productId, UUID warehouseId) {
        productService.ensureActive(productId);
        warehouseService.ensureActive(warehouseId);
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Header %s wajib dikirim".formatted(InventoryConstants.IDEMPOTENCY_KEY_HEADER));
        }
        String key = idempotencyKey.trim();
        if (key.length() > InventoryConstants.IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key maksimal %d karakter".formatted(InventoryConstants.IDEMPOTENCY_KEY_MAX_LENGTH));
        }
        return key;
    }

    /**
     * Menjalankan mutasi pada transaksi milik {@link InventoryMutationService}.
     *
     * <p>Bila dua request dengan {@code Idempotency-Key} sama tiba bersamaan, keduanya lolos
     * pre-check; salah satunya gagal pada unique constraint dan transaksinya di-rollback.
     * Yang kalah kemudian membaca transaksi pemenang di transaksi baru dan mengembalikan hasil
     * yang sama (FSD §6.1 baris 4).
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
            return inventoryMutationService.findReused(command)
                    .orElseThrow(() -> new ConcurrencyConflictException(command.idempotencyKey()));
        }
    }

    /**
     * Menelusuri rantai {@code cause} untuk mencari pelanggaran unique {@code idempotency_key}.
     *
     * <p>Perulangan manual dipertahankan (bukan stream): yang ditelusuri adalah rantai
     * {@link Throwable}, bukan koleksi.
     */
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
