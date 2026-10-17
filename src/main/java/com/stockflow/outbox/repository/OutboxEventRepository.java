package com.stockflow.outbox.repository;

import com.stockflow.outbox.entity.OutboxEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;

/**
 * Penyimpanan event outbox.
 *
 * <p>Method {@link #append(OutboxEvent)} <b>tidak</b> dianotasi {@code @Transactional} dengan
 * sengaja: penulisan outbox harus menjadi bagian dari transaksi milik use case pemanggil
 * ({@code InventoryMutationService}) sehingga perubahan stok, histori, dan event tersimpan
 * secara atomik.
 */
public interface OutboxEventRepository extends PanacheRepository<OutboxEvent> {

    /** Menyimpan event dan langsung {@code flush} di dalam transaksi pemanggil. */
    OutboxEvent append(OutboxEvent event);

    /** Membaca event yang belum terpublikasi (dipakai publisher Phase 4). */
    List<OutboxEvent> findPending(int limit);

    long countPending();
}
