package com.stockflow.inventory.service;

import com.stockflow.common.exception.IdempotencyKeyReusedException;
import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InventoryNotFoundException;
import com.stockflow.inventory.entity.InventoryTransaction;
import com.stockflow.inventory.entity.TransactionType;
import com.stockflow.inventory.mapper.StockEventPayloadMapper;
import com.stockflow.inventory.repository.InventoryMutation;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.repository.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Batas transaksi database untuk mutasi stok.</b>
 *
 * <p>Satu method = satu use case = <b>satu transaksi PostgreSQL</b> yang mencakup:
 *
 * <ol>
 *   <li>pemeriksaan {@code Idempotency-Key} (di dalam transaksi agar pemeriksaan dan
 *       penulisan tidak dapat disela request lain);
 *   <li>mutasi {@code inventory} lewat satu statement SQL atomik;
 *   <li>penulisan histori ke {@code inventory_transactions};
 *   <li>penulisan event ke {@code outbox_events}.
 * </ol>
 *
 * <p>Konsekuensinya: setiap kegagalan pada langkah mana pun menggagalkan seluruh transaksi —
 * tidak ada stok yang berubah tanpa histori, dan tidak ada event yang ditulis tanpa mutasi.
 *
 * <p>Kelas ini sengaja dipisahkan dari {@link InventoryService} (orkestrator yang
 * <b>tidak</b> transaksional). Pemisahan ini wajib: bila penanganan balapan
 * {@code Idempotency-Key} berada di dalam transaksi yang sama, transaksi sudah
 * <i>rollback-only</i> setelah {@code flush()} gagal sehingga pembacaan transaksi pemenang
 * tidak mungkin dilakukan (architecture-plan §4.3).
 */
@ApplicationScoped
public class InventoryMutationService {

    private static final Logger LOG = Logger.getLogger(InventoryMutationService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockEventPayloadMapper payloadMapper;

    public InventoryMutationService(InventoryRepository inventoryRepository,
                                    InventoryTransactionRepository inventoryTransactionRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    StockEventPayloadMapper payloadMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.payloadMapper = payloadMapper;
    }

    @Transactional
    public StockMutationResult applyStockIn(StockMutationCommand command) {
        Optional<StockMutationResult> previous = findReused(command);
        if (previous.isPresent()) {
            return previous.get();
        }

        InventoryMutation mutation = inventoryRepository.increaseQuantity(
                command.productId(), command.warehouseId(), command.quantity());

        long quantityBefore = mutation.quantityAfter() - command.quantity();
        return record(command, TransactionType.IN, mutation.inventoryId(), quantityBefore, mutation.quantityAfter());
    }

    @Transactional
    public StockMutationResult applyStockOut(StockMutationCommand command) {
        Optional<StockMutationResult> previous = findReused(command);
        if (previous.isPresent()) {
            return previous.get();
        }

        Optional<InventoryMutation> mutation = inventoryRepository.decreaseQuantityIfEnough(
                command.productId(), command.warehouseId(), command.quantity());

        if (mutation.isEmpty()) {
            // 0 baris: bisa karena stok tidak cukup, atau karena baris inventory belum ada.
            if (!inventoryRepository.existsFor(command.productId(), command.warehouseId())) {
                throw new InventoryNotFoundException(command.productId(), command.warehouseId());
            }
            throw new InsufficientStockException(command.productId(), command.warehouseId(), command.quantity());
        }

        InventoryMutation applied = mutation.get();
        long quantityBefore = applied.quantityAfter() + command.quantity();
        return record(command, TransactionType.OUT, applied.inventoryId(), quantityBefore, applied.quantityAfter());
    }

    /**
     * Mencari hasil mutasi sebelumnya untuk {@code Idempotency-Key} yang sama.
     *
     * <p>Payload identik → hasil lama dikembalikan tanpa mutasi baru. Payload berbeda →
     * {@code 409 IDEMPOTENCY_KEY_REUSED}. Method ini juga dipakai {@link InventoryService}
     * untuk menangani request kembar yang berbarengan (FSD §6.1).
     */
    @Transactional
    public Optional<StockMutationResult> findReused(StockMutationCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            return Optional.empty();
        }
        return inventoryTransactionRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(transaction -> {
                    if (transaction.getPayloadHash() != null
                            && !transaction.getPayloadHash().equals(command.payloadHash())) {
                        throw new IdempotencyKeyReusedException(command.idempotencyKey());
                    }
                    return StockMutationResult.reused(transaction);
                });
    }

    private StockMutationResult record(StockMutationCommand command, TransactionType type,
                                       UUID inventoryId, long quantityBefore, long quantityAfter) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long quantityDelta = quantityAfter - quantityBefore;

        InventoryTransaction transaction = InventoryTransaction.builder()
                .id(UUID.randomUUID())
                .productId(command.productId())
                .warehouseId(command.warehouseId())
                .transactionType(type)
                .quantityDelta(quantityDelta)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceId(command.referenceId())
                .idempotencyKey(command.idempotencyKey())
                .payloadHash(command.payloadHash())
                .notes(command.notes())
                .createdAt(now)
                .build();

        // flush di dalam boundary transaksi: unique violation (mis. balapan Idempotency-Key)
        // muncul di sini, bukan saat commit, sehingga bisa ditangani InventoryService.
        inventoryTransactionRepository.persistAndFlush(transaction);

        UUID eventId = UUID.randomUUID();
        String payload = payloadMapper.payload(eventId, inventoryId, type, transaction);
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType(payloadMapper.aggregateType())
                .aggregateId(inventoryId)
                .eventType(type.eventType())
                .payload(payload)
                .createdAt(now)
                .build();
        outboxEventRepository.append(event);

        LOG.infof("mutasi stok tercatat type=%s inventoryId=%s before=%d delta=%d after=%d transactionId=%s eventId=%s",
                type, inventoryId, quantityBefore, quantityDelta, quantityAfter, transaction.getId(), eventId);

        return StockMutationResult.of(transaction.getId(), eventId, inventoryId,
                command.productId(), command.warehouseId(), quantityBefore, quantityDelta, quantityAfter);
    }
}
