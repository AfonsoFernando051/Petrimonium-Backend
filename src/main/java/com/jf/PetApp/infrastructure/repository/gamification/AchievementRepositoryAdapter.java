package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.infrastructure.entity.AchievementUnlockJpaEntity;

/**
 * The only place in the codebase that knows achievement unlocks are stored
 * as a JPA entity. Implements {@link AchievementRepositoryPort} so
 * {@code EvaluateAchievementsUseCaseImpl} never touches Spring Data directly.
 */
@Repository
public class AchievementRepositoryAdapter implements AchievementRepositoryPort {

    private final AchievementUnlockJpaRepository repository;

    public AchievementRepositoryAdapter(AchievementUnlockJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isUnlocked(Long userId, String achievementCode) {
        return repository.existsByUserIdAndAchievementCode(userId, achievementCode);
    }

    @Override
    @Transactional
    public void unlock(Long userId, String achievementCode, int xpAwarded) {
        if (repository.existsByUserIdAndAchievementCode(userId, achievementCode)) {
            return;
        }
        AchievementUnlockJpaEntity entity = new AchievementUnlockJpaEntity();
        entity.setUserId(userId);
        entity.setAchievementCode(achievementCode);
        entity.setXpAwarded(xpAwarded);
        entity.setUnlockedAt(Instant.now());
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // A concurrent duplicate request already unlocked this achievement between our
            // exists-check and this insert — the unique constraint caught it. Treat it as
            // the idempotent no-op it actually is, not a 500.
        }
    }

    @Override
    public Map<String, Instant> unlockedWithTimestamps(Long userId) {
        return repository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        AchievementUnlockJpaEntity::getAchievementCode,
                        AchievementUnlockJpaEntity::getUnlockedAt));
    }

    @Override
    public int totalXpFor(Long userId) {
        return repository.sumXpAwardedByUserId(userId);
    }
}
