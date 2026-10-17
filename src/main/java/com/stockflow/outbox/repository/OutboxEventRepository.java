package com.stockflow.outbox.repository;

import com.stockflow.outbox.entity.OutboxEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Penyimpanan event outbox.
 *
 * <p>Method {@link #append(OutboxEvent)} <b>tidak</b> dianotasi {@code @Transactional} dengan
 * sengaja: penulisan outbox harus menjadi bagian dari transaksi milik use case pemanggil
 * ({@code InventoryMutationService}) sehingga perubahan stok, histori, dan event tersimpan
 * secara atomik.
 *
 * <p>Method klaim ({@link #claimPending(int, long)}) memakai {@code FOR UPDATE SKIP LOCKED}
 * sehingga beberapa publisher tidak memproses event yang sama (architecture-plan §7.2 butir 4).
 */
public interface OutboxEventRepository extends PanacheRepository<OutboxEvent> {

    /** Menyimpan event dan langsung {@code flush} di dalam transaksi pemanggil. */
    OutboxEvent append(OutboxEvent event);

    /** Membaca event yang belum terpublikasi (dipakai publisher / metrik). */
    List<OutboxEvent> findPending(int limit);

    long countPending();

    /**
     * Mengklaim sampai {@code pageSize} event pending dan mengunci barisnya
     * ({@code FOR UPDATE SKIP LOCKED}) sampai transaksi pemanggil selesai.
     *
     * <p>{@code leaseSeconds} menentukan berapa lama baris dapat dipegang sebelum boleh diklaim
     * ulang oleh publisher lain (FSD §10.3 {@code lock-lease}).
     */
    List<OutboxEvent> claimPending(int pageSize, long leaseSeconds);

    /** Menandai event terpublikasi dan melepas lease-nya. */
    void markPublished(UUID id, OffsetDateTime publishedAt);

    /** Menambah counter percobaan, menyimpan pesan error, dan melepas lease-nya. */
    void markFailed(UUID id, String errorMessage);
}
