package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.Instant;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.core.domain.gamification.XpEventType;
import com.jf.PetApp.infrastructure.entity.XpEventJpaEntity;

/**
 * The only place in the codebase that knows the XP ledger is stored as a
 * JPA entity. Implements {@link XpEventRepositoryPort} so
 * {@code XpLedgerService} never touches Spring Data directly.
 */
@Repository
public class XpEventRepositoryAdapter implements XpEventRepositoryPort {

    private final XpEventJpaRepository repository;

    public XpEventRepositoryAdapter(XpEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByUserIdAndEventTypeAndSourceId(Long userId, XpEventType eventType, String sourceId) {
        return repository.existsByUserIdAndEventTypeAndSourceId(userId, eventType, sourceId);
    }

    @Override
    @Transactional
    public void save(Long userId, XpEventType eventType, int amount, String sourceId) {
        XpEventJpaEntity entity = new XpEventJpaEntity();
        entity.setUserId(userId);
        entity.setEventType(eventType);
        entity.setAmount(amount);
        entity.setSourceId(sourceId);
        entity.setCreatedAt(Instant.now());
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // A concurrent duplicate request already inserted this (userId, eventType,
            // sourceId) between our exists-check and this insert — the unique constraint
            // caught it. Treat it as the idempotent no-op it actually is, not a 500.
        }
    }

    @Override
    public int sumAmountByUserId(Long userId) {
        return repository.sumAmountByUserId(userId);
    }

    @Override
    public int countByUserIdAndEventTypeAndCreatedAtBetween(
            Long userId, XpEventType eventType, Instant fromInclusive, Instant toExclusive) {
        return repository.countByUserIdAndEventTypeAndCreatedAtBetween(userId, eventType, fromInclusive, toExclusive);
    }

    @Override
    public Set<String> sourceIdsByUserIdAndEventType(Long userId, XpEventType eventType) {
        return repository.sourceIdsByUserIdAndEventType(userId, eventType);
    }
}
