package com.stockflow.outbox.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.outbox.entity.OutboxEvent;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Adapter Kafka untuk publisher outbox (FSD §6.3, §10.2).
 *
 * <p>Key message mengikuti aturan §6.3 — {@code {productId}:{warehouseId}} — agar event untuk
 * kombinasi produk dan gudang yang sama masuk ke partisi yang sama. Kolom outbox hanya
 * menyimpan identitas aggregate, sehingga key diturunkan dari payload event.
 *
 * <p>Pengiriman bersifat <b>sinkron menunggu ack</b>: event baru ditandai {@code published}
 * setelah broker meng-ack, sesuai arsitektur §7.1 (publikasi setelah commit DB, ack sebelum
 * penandaan). Akibatnya proses yang berhenti antara ack dan commit menyebabkan event dipublikasikan
 * ulang — ini <i>at-least-once</i> (§6.4) dan consumer wajib idempotent berbasis {@code eventId}.
 */
@ApplicationScoped
public class KafkaOutboxEventGateway implements OutboxEventGateway {

    private static final Logger LOG = Logger.getLogger(KafkaOutboxEventGateway.class);

    @Channel("stock-events")
    Emitter<KafkaRecord<String, String>> emitter;

    private final ObjectMapper objectMapper;

    @Inject
    public KafkaOutboxEventGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean publish(OutboxEvent event) {
        String key = keyOf(event);
        // Menunggu ack broker; gagal = exception CompletionException yang ditangkap publisher.
        emitter.send(KafkaRecord.of(key, event.getPayload()))
                .toCompletableFuture()
                .join();
        LOG.debugf("event terkirim ke Kafka key=%s eventId=%s", key, event.getId());
        return true;
    }

    private String keyOf(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            JsonNode productId = payload.get("productId");
            JsonNode warehouseId = payload.get("warehouseId");
            if (productId != null && warehouseId != null) {
                return productId.asText() + ":" + warehouseId.asText();
            }
        } catch (JsonProcessingException e) {
            LOG.warnf("payload eventId=%s tidak dapat dibaca untuk key, memakai aggregateId",
                    event.getId());
        }
        return event.getAggregateId().toString();
    }
}
