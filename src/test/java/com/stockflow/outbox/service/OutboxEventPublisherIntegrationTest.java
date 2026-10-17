package com.stockflow.outbox.service;

import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.gateway.KafkaOutboxEventGateway;
import com.stockflow.outbox.repository.OutboxEventRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integrasi publisher outbox terhadap database nyata.
 *
 * <p>{@link KafkaOutboxEventGateway} di-mock agar tidak ada koneksi broker selama test.
 *
 * <p>Assertion selalu dilakukan pada <b>event spesifik</b> yang dibuat test, bukan pada jumlah
 * event global — event sisa dari kelas test lain (yang tidak memakai {@code @TestTransaction})
 * tetap ada di tabel dan tidak boleh memengaruhi hasil.
 *
 * <p>Catatan: {@code markPublished}/{@code markFailed} memakai <i>bulk update</i> (tidak menyentuh
 * entity di persistence context), sehingga entity dibaca ulang dengan {@code refresh} setelah
 * publisher dijalankan.
 */
@QuarkusTest
@TestTransaction
class OutboxEventPublisherIntegrationTest {

    @Inject
    OutboxEventRepository repository;

    @Inject
    OutboxEventPublisher publisher;

    @InjectMock
    KafkaOutboxEventGateway gateway; // menggantikan bean Kafka → tanpa koneksi broker

    @BeforeEach
    void bersihkanOutbox() {
        repository.deleteAll();
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Inventory")
                .aggregateId(UUID.randomUUID())
                .eventType("StockInCompleted")
                .payload("{\"productId\":\"%s\",\"warehouseId\":\"%s\",\"eventType\":\"StockInCompleted\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID()))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        repository.append(event);
        return event;
    }

    private OutboxEvent reload(OutboxEvent event) {
        OutboxEvent reloaded = repository.find("id", event.getId()).firstResult();
        repository.getEntityManager().refresh(reloaded);
        return reloaded;
    }

    @Test
    @DisplayName("event pending ditandai published setelah gateway meng-ack")
    void eventPendingDitandaiPublished() {
        OutboxEvent event = pendingEvent();
        Mockito.when(gateway.publish(Mockito.any())).thenReturn(true);

        publisher.publishPending();

        OutboxEvent reloaded = reload(event);
        assertNotNull(reloaded.getPublishedAt());
        assertEquals(0, reloaded.getAttempts());
        assertNull(reloaded.getLastError());
    }

    @Test
    @DisplayName("event gagal → attempts naik, lastError terisi, published_at tetap NULL")
    void eventGagalAttemptsNaik() {
        OutboxEvent event = pendingEvent();
        Mockito.when(gateway.publish(Mockito.any())).thenReturn(false);

        publisher.publishPending();

        OutboxEvent reloaded = reload(event);
        assertNull(reloaded.getPublishedAt());
        assertEquals(1, reloaded.getAttempts());
        assertNotNull(reloaded.getLastError());
    }

    @Test
    @DisplayName("event yang sudah published tidak diproses ulang pada siklus berikutnya")
    void eventPublishedTidakDiprosesUlang() {
        OutboxEvent event = pendingEvent();
        Mockito.when(gateway.publish(Mockito.any())).thenReturn(true);
        publisher.publishPending();

        OutboxEvent setelahPublish = reload(event);
        assertNotNull(setelahPublish.getPublishedAt());
        assertEquals(0, setelahPublish.getAttempts());

        // siklus kedua: event sudah published → tidak diklaim, attempts tidak berubah
        publisher.publishPending();

        OutboxEvent setelahSiklusKedua = reload(event);
        assertEquals(setelahPublish.getPublishedAt(), setelahSiklusKedua.getPublishedAt());
        assertEquals(0, setelahSiklusKedua.getAttempts());
    }
}
