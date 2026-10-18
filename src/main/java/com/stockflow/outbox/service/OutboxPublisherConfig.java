package com.stockflow.outbox.service;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/**
 * Konfigurasi publisher outbox (FSD §10.3).
 *
 * <p>Prefix {@code outbox.publisher} — seluruh nilai dapat ditimpa lewat environment variable
 * (mis. {@code OUTBOX_PUBLISHER_ENABLED}) tanpa mengubah kode.
 */
@ConfigMapping(prefix = "outbox.publisher")
public interface OutboxPublisherConfig {

    /** Kill switch: publisher tidak memproses event bila {@code false}. */
    @WithDefault("true")
    boolean enabled();

    /** Interval antar siklus publikasi. */
    @WithDefault("1m")
    Duration pollInterval();

    /** Jumlah event yang diklaim per siklus. */
    @WithDefault("50")
    int pageSize();

    /** Batas percobaan sebelum event dianggap gagal permanen (perlu intervensi/DLQ). */
    @WithDefault("10")
    int maxAttempts();

    /** Lease klaim event — menangani publisher yang berhenti mendadak sebelum commit. */
    @WithDefault("30s")
    Duration lockLease();
}
