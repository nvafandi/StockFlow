package com.stockflow.inventory.infrastructure;

import com.stockflow.common.exception.IdempotencyKeyReusedException;
import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InventoryNotFoundException;
import com.stockflow.inventory.domain.InventoryTransaction;
import com.stockflow.inventory.domain.StockMutationCommand;
import com.stockflow.inventory.domain.StockMutationResult;
import com.stockflow.inventory.domain.TransactionType;
import com.stockflow.outbox.domain.OutboxEvent;
import com.stockflow.outbox.infrastructure.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
 * Application service hanya memanggil method ini dan tidak membuka transaksi sendiri
 * (architecture-plan §4.1).
 */
@ApplicationScoped
public class StockMutationRepository {

    private static final Logger LOG = Logger.getLogger(StockMutationRepository.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StockEventPayloadFactory payloadFactory;

    @Inject
    public StockMutationRepository(InventoryRepository inventoryRepository,
                                   InventoryTransactionRepository inventoryTransactionRepository,
                                   OutboxEventRepository outboxEventRepository,
                                   StockEventPayloadFactory payloadFactory) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.payloadFactory = payloadFactory;
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
     * {@code 409 IDEMPOTENCY_KEY_REUSED}. Method ini juga dipakai application service untuk
     * menangani request kembar yang berbarengan (FSD §6.1).
     */
    public Optional<StockMutationResult> findReused(StockMutationCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            return Optional.empty();
        }
        return inventoryTransactionRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(transaction -> {
                    if (transaction.payloadHash != null && !transaction.payloadHash.equals(command.payloadHash())) {
                        throw new IdempotencyKeyReusedException(command.idempotencyKey());
                    }
                    return StockMutationResult.reused(transaction);
                });
    }

    private StockMutationResult record(StockMutationCommand command, TransactionType type,
                                       UUID inventoryId, long quantityBefore, long quantityAfter) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long quantityDelta = quantityAfter - quantityBefore;

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.id = UUID.randomUUID();
        transaction.productId = command.productId();
        transaction.warehouseId = command.warehouseId();
        transaction.transactionType = type;
        transaction.quantityDelta = quantityDelta;
        transaction.quantityBefore = quantityBefore;
        transaction.quantityAfter = quantityAfter;
        transaction.referenceId = command.referenceId();
        transaction.idempotencyKey = command.idempotencyKey();
        transaction.payloadHash = command.payloadHash();
        transaction.notes = command.notes();
        transaction.createdAt = now;

        // flush di dalam boundary transaksi: unique violation (mis. balapan Idempotency-Key)
        // muncul di sini, bukan saat commit, sehingga bisa ditangani application service.
        inventoryTransactionRepository.persistAndFlush(transaction);

        UUID eventId = UUID.randomUUID();
        String payload = payloadFactory.payload(eventId, inventoryId, type, transaction);
        OutboxEvent event = OutboxEvent.pending(eventId, payloadFactory.aggregateType(), inventoryId,
                payloadFactory.eventType(type), payload, now);
        outboxEventRepository.append(event);

        LOG.infof("mutasi stok tercatat type=%s inventoryId=%s before=%d delta=%d after=%d transactionId=%s eventId=%s",
                type, inventoryId, quantityBefore, quantityDelta, quantityAfter, transaction.id, eventId);

        return StockMutationResult.of(transaction.id, eventId, inventoryId,
                command.productId(), command.warehouseId(), quantityBefore, quantityDelta, quantityAfter);
    }
}
