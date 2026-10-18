package com.stockflow.outbox.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
 *
 * <p>Indeks {@code idx_outbox_pending} pada Flyway V5 bersifat <i>partial</i>
 * ({@code WHERE published_at IS NULL}); predikat partial tidak dapat dinyatakan lewat
 * {@code @Index} JPA, sehingga di sini hanya kolomnya yang didokumentasikan.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        @Index(name = "idx_outbox_pending", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = "payload")
public class OutboxEvent extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "leased_at")
    private OffsetDateTime leasedAt;
}
