package com.stockflow.outbox.service;

import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.gateway.OutboxEventGateway;
import com.stockflow.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test murni untuk {@link OutboxEventPublisher} (tanpa Quarkus, tanpa database).
 *
 * <p>Repository dan gateway di-mock sehingga aturan klaim/ack/attempt dapat diuji secara
 * deterministik — termasuk jalur kegagalan yang di produksi hanya terjadi saat broker down.
 */
class OutboxEventPublisherTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxEventGateway gateway = mock(OutboxEventGateway.class);
    private final OutboxPublisherConfig config = mock(OutboxPublisherConfig.class);
    private final OutboxEventPublisher publisher = new OutboxEventPublisher(repository, gateway, config);

    private void configAktif() {
        when(config.enabled()).thenReturn(true);
        when(config.pageSize()).thenReturn(50);
        when(config.lockLease()).thenReturn(Duration.ofSeconds(30));
        when(config.maxAttempts()).thenReturn(10);
    }

    private OutboxEvent pendingEvent(long attempts) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Inventory")
                .aggregateId(UUID.randomUUID())
                .eventType("StockInCompleted")
                .payload("{\"productId\":\"%s\",\"warehouseId\":\"%s\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID()))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .attempts((int) attempts)
                .build();
    }

    @Test
    @DisplayName("publisher dinonaktifkan via kill switch → repository & gateway tidak disentuh")
    void publisherNonAktifTidakMelakukanApaPun() {
        when(config.enabled()).thenReturn(false);

        publisher.publishPending();

        verifyNoInteractions(repository, gateway);
    }

    @Test
    @DisplayName("tidak ada event pending → gateway tidak dipanggil")
    void tanpaEventPendingTidakMemanggilGateway() {
        configAktif();
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of());

        publisher.publishPending();

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("ack dari broker → event ditandai published")
    void eventBerhasilDitandaiPublished() {
        configAktif();
        OutboxEvent event = pendingEvent(0);
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of(event));
        when(gateway.publish(any())).thenReturn(true);

        publisher.publishPending();

        verify(repository).markPublished(eq(event.getId()), any(OffsetDateTime.class));
        verify(repository, never()).markFailed(any(), anyString());
    }

    @Test
    @DisplayName("broker tidak meng-ack → attempts naik dan lastError disimpan")
    void eventGagalBrokerTidakAck() {
        configAktif();
        OutboxEvent event = pendingEvent(2);
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of(event));
        when(gateway.publish(any())).thenReturn(false);

        publisher.publishPending();

        verify(repository).markFailed(eq(event.getId()), anyString());
        verify(repository, never()).markPublished(any(), any());
    }

    @Test
    @DisplayName("exception dari gateway ditangkap → event ditandai gagal tanpa menghentikan siklus")
    void exceptionGatewayDitangkap() {
        configAktif();
        OutboxEvent event = pendingEvent(0);
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of(event));
        when(gateway.publish(any())).thenThrow(new IllegalStateException("broker tidak dapat dijangkau"));

        assertDoesNotThrow(() -> publisher.publishPending());

        verify(repository).markFailed(eq(event.getId()), anyString());
    }

    @Test
    @DisplayName("kegagalan satu event tidak menghentikan pemrosesan event berikutnya")
    void kegagalanSatuEventTidakMenghentikanSiklus() {
        configAktif();
        OutboxEvent first = pendingEvent(0);
        OutboxEvent second = pendingEvent(0);
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of(first, second));
        when(gateway.publish(first)).thenThrow(new IllegalStateException("timeout"));
        when(gateway.publish(second)).thenReturn(true);

        publisher.publishPending();

        verify(repository).markFailed(eq(first.getId()), anyString());
        verify(repository).markPublished(eq(second.getId()), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("event mencapai batas percobaan → tetap ditandai gagal (intervensi/DLQ dilaporkan)")
    void eventMencapaiBatasPercobaan() {
        configAktif();
        OutboxEvent event = pendingEvent(9); // attempts 9 + 1 = 10 → mencapai maxAttempts
        when(repository.claimPending(anyInt(), anyLong())).thenReturn(List.of(event));
        when(gateway.publish(any())).thenReturn(false);

        publisher.publishPending();

        verify(repository).markFailed(eq(event.getId()), anyString());
    }
}
