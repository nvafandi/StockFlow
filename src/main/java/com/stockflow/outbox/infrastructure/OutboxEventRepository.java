package com.stockflow.outbox.infrastructure;

import com.stockflow.outbox.domain.OutboxEvent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Penyimpanan event outbox.
 *
 * <p>Method {@link #append(OutboxEvent)} <b>tidak</b> dianotasi {@code @Transactional} dengan
 * sengaja: penulisan outbox harus menjadi bagian dari transaksi milik use case pemanggil
 * (yaitu {@code StockMutationRepository}) sehingga perubahan stok, histori, dan event
 * tersimpan secara atomik.
 */
@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEvent> {

    public OutboxEvent append(OutboxEvent event) {
        persistAndFlush(event);
        return event;
    }

    public List<OutboxEvent> findPending(int limit) {
        return find("publishedAt is null order by createdAt").page(0, limit).list();
    }

    public long countPending() {
        return count("publishedAt is null");
    }
}
