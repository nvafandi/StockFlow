package com.stockflow.outbox.service;

import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.gateway.OutboxEventGateway;
import com.stockflow.outbox.repository.OutboxEventRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Transactional outbox publisher (FSD §10.3, architecture-plan §6–§7).
 *
 * <p>Satu siklus publikasi:
 *
 * <ol>
 *   <li>mengklaim sampai {@code page-size} event pending dengan {@code FOR UPDATE SKIP LOCKED}
 *       — beberapa publisher dapat berjalan bersamaan tanpa memproses event yang sama
 *       (§7.2 butir 4);</li>
 *   <li>mengirim tiap event ke broker dan <b>menunggu ack</b>;</li>
 *   <li>ack → event ditandai {@code published}; gagal → {@code attempts} naik dan
 *       {@code last_error} disimpan.</li>
 * </ol>
 *
 * <p>Seluruh siklus berjalan dalam <b>satu transaksi</b>: ack dan penandaan status dilakukan
 * di dalam transaksi yang sama dengan klaim, sehingga event tidak hilang di tengah jalan.
 * Kegagalan pengiriman tidak menggagalkan siklus — event tetap di outbox dan dicoba kembali
 * pada siklus berikutnya (at-least-once, §6.4).
 *
 * <p>Response HTTP ke client <b>tidak</b> menunggu publisher (§7.2 butir 5) — publikasi terjadi
 * setelah commit transaksi use case.
 */
@ApplicationScoped
public class OutboxEventPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository repository;
    private final OutboxEventGateway gateway;
    private final OutboxPublisherConfig config;

    public OutboxEventPublisher(OutboxEventRepository repository,
                                OutboxEventGateway gateway,
                                OutboxPublisherConfig config) {
        this.repository = repository;
        this.gateway = gateway;
        this.config = config;
    }

    /**
     * Menjalankan satu siklus publikasi. Dipanggil scheduler setiap
     * {@code outbox.publisher.poll-interval}; juga dapat dipanggil langsung dari test.
     *
     * <p>Tidak memakai {@code delay} agar eksekusi pertama terjadi setelah satu interval
     * (bukan 10 detik setelah startup) — selama test, interval diatur {@code off} pada
     * profil test sehingga scheduler tidak menyentuh database.
     */
    @Scheduled(every = "${outbox.publisher.poll-interval:1m}")
    @Transactional
    public void publishPending() {
        if (!config.enabled()) {
            return;
        }

        List<OutboxEvent> claimed = repository.claimPending(config.pageSize(), config.lockLease().toSeconds());
        if (claimed.isEmpty()) {
            return;
        }

        LOG.infof("outbox publisher: %d event diklaim untuk dipublikasikan", claimed.size());
        for (OutboxEvent event : claimed) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            boolean acknowledged = gateway.publish(event);
            if (acknowledged) {
                repository.markPublished(event.getId(), OffsetDateTime.now(ZoneOffset.UTC));
                return;
            }
            repository.markFailed(event.getId(), "broker tidak meng-ack event");
        } catch (RuntimeException e) {
            repository.markFailed(event.getId(), e.getMessage());
            LOG.warnf(e, "outbox publisher: gagal memublikasikan eventId=%s", event.getId());
        }

        if (event.getAttempts() + 1 >= config.maxAttempts()) {
            LOG.errorf("outbox publisher: eventId=%s mencapai batas percobaan (%d) — "
                    + "perlu intervensi/DLQ (FSD §10.3)", event.getId(), config.maxAttempts());
        }
    }
}
