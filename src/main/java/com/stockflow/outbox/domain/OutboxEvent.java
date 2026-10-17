package com.stockflow.outbox.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactional outbox: event yang harus dipublikasikan ke Kafka, ditulis pada transaksi
 * database yang sama dengan perubahan stok (architecture-plan §7).
 *
 * <p>Entity ini sengaja tidak mengetahui Kafka sama sekali; publisher dijalankan terpisah
 * pada Phase 4.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    public String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    public UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    public String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    public String payload;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "published_at")
    public OffsetDateTime publishedAt;

    @Column(name = "attempts", nullable = false)
    public int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    public String lastError;

    public static OutboxEvent pending(UUID id, String aggregateType, UUID aggregateId,
                                      String eventType, String payload, OffsetDateTime createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.id = id;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.createdAt = createdAt;
        event.publishedAt = null;
        event.attempts = 0;
        event.lastError = null;
        return event;
    }
}
