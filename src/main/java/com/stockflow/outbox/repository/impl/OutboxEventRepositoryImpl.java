package com.stockflow.outbox.repository.impl;

import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.repository.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Implementasi {@link OutboxEventRepository} berbasis Panache.
 */
@ApplicationScoped
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    @Override
    public OutboxEvent append(OutboxEvent event) {
        persistAndFlush(event);
        return event;
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return find("publishedAt is null order by createdAt").page(0, limit).list();
    }

    @Override
    public long countPending() {
        return count("publishedAt is null");
    }
}
