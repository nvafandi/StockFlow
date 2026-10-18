package com.stockflow.outbox.gateway;

import com.stockflow.outbox.entity.OutboxEvent;

/**
 * Porta pengiriman event outbox ke message broker.
 *
 * <p>Publisher tidak mengetahui teknologi broker (Dependency Inversion): adapter
 * {@code KafkaOutboxEventGateway} yang berbicara dengan Kafka. Dipakai pada unit test
 * sebagai mock sehingga perilaku publisher dapat diuji tanpa broker.
 */
public interface OutboxEventGateway {

    /**
     * Mengirim satu event ke broker dan menunggu acknowledgment.
     *
     * @return {@code true} bila broker meng-ack; {@code false} bila event tidak terkirim tetapi
     *         kegagalan bersifat dapat dipulihkan.
     * @throws RuntimeException bila pengiriman gagal (broker menolak, timeout, dsb.).
     *         Publisher menangkapnya dan menaikkan counter percobaan.
     */
    boolean publish(OutboxEvent event);
}
