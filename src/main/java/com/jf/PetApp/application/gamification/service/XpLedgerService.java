package com.jf.PetApp.application.gamification.service;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.core.domain.gamification.XpEventType;

/**
 * The single place that grants XP. Every grant is idempotent on
 * {@code (userId, eventType, sourceId)} — calling {@link #grantXp} twice for
 * the same lesson/module never awards XP twice, so retried or duplicate
 * requests are always safe.
 */
@Service
public class XpLedgerService {

    private final XpEventRepositoryPort xpEventRepository;

    public XpLedgerService(XpEventRepositoryPort xpEventRepository) {
        this.xpEventRepository = xpEventRepository;
    }

    /**
     * Grants {@code amount} XP for {@code eventType}/{@code sourceId} unless
     * it was already granted. Returns whether this call actually granted XP
     * (false means it was a no-op repeat).
     */
    public boolean grantXp(Long userId, XpEventType eventType, int amount, String sourceId) {
        if (xpEventRepository.existsByUserIdAndEventTypeAndSourceId(userId, eventType, sourceId)) {
            return false;
        }
        xpEventRepository.save(userId, eventType, amount, sourceId);
        return true;
    }

    public int totalXpFor(Long userId) {
        return xpEventRepository.sumAmountByUserId(userId);
    }
}
