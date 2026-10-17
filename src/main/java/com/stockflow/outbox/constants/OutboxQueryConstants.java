package com.stockflow.outbox.constants;

/**
 * Literal query modul outbox.
 *
 * <p>Semua SQL/JPQL dikumpulkan di sini agar tidak ada string query yang tersebar di
 * repository (standar repository &amp; constants).
 */
public final class OutboxQueryConstants {

    /**
     * Klaim event pending dengan {@code FOR UPDATE SKIP LOCKED}.
     *
     * <p>Pola ini memenuhi §7.2 butir 4 (architecture-plan): beberapa publisher dapat berjalan
     * bersamaan tanpa memproses event yang sama — baris yang sedang dipegang transaksi lain
     * dilewati, bukan ditunggu. {@code leased_at} menangani publisher yang berhenti mendadak
     * sebelum commit: setelah lease kedaluwarsa, event dapat diklaim ulang.
     *
     * <p>{@code make_interval(secs => :leaseSeconds)} membatasi klaim pada event yang
     * {@code leased_at IS NULL} atau lease-nya sudah kedaluwarsa.
     */
    public static final String CLAIM_PENDING_SQL = """
            SELECT id, aggregate_type, aggregate_id, event_type, payload,
                   created_at, published_at, attempts, last_error, leased_at
              FROM outbox_events
             WHERE published_at IS NULL
               AND (leased_at IS NULL OR leased_at < now() - make_interval(secs => :leaseSeconds))
             ORDER BY created_at
             LIMIT :pageSize
               FOR UPDATE SKIP LOCKED
            """;

    private OutboxQueryConstants() {
    }
}
