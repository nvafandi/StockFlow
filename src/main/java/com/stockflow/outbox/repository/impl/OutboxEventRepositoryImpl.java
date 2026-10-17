package com.stockflow.outbox.repository.impl;

import com.stockflow.outbox.constants.OutboxQueryConstants;
import com.stockflow.outbox.entity.OutboxEvent;
import com.stockflow.outbox.repository.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

    @Override
    public List<OutboxEvent> claimPending(int pageSize, long leaseSeconds) {
        return getEntityManager()
                .createNativeQuery(OutboxQueryConstants.CLAIM_PENDING_SQL, OutboxEvent.class)
                .setParameter("leaseSeconds", leaseSeconds)
                .setParameter("pageSize", pageSize)
                .getResultList();
    }

    @Override
    public void markPublished(UUID id, OffsetDateTime publishedAt) {
        update("publishedAt = ?1, leasedAt = null where id = ?2", publishedAt, id);
    }

    @Override
    public void markFailed(UUID id, String errorMessage) {
        update("attempts = attempts + 1, lastError = ?1, leasedAt = null where id = ?2", errorMessage, id);
    }
}
