package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.infrastructure.entity.MissionCompletionJpaEntity;

/**
 * The only place in the codebase that knows mission completions are stored
 * as a JPA entity. Implements {@link MissionRepositoryPort} so
 * {@code EvaluateMissionsUseCaseImpl} never touches Spring Data directly.
 */
@Repository
public class MissionRepositoryAdapter implements MissionRepositoryPort {

    private final MissionCompletionJpaRepository repository;

    public MissionRepositoryAdapter(MissionCompletionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isCompleted(Long userId, String missionCode, String periodKey) {
        return repository.existsByUserIdAndMissionCodeAndPeriodKey(userId, missionCode, periodKey);
    }

    @Override
    @Transactional
    public void complete(Long userId, String missionCode, String periodKey, int xpAwarded) {
        if (repository.existsByUserIdAndMissionCodeAndPeriodKey(userId, missionCode, periodKey)) {
            return;
        }
        MissionCompletionJpaEntity entity = new MissionCompletionJpaEntity();
        entity.setUserId(userId);
        entity.setMissionCode(missionCode);
        entity.setPeriodKey(periodKey);
        entity.setXpAwarded(xpAwarded);
        entity.setCompletedAt(Instant.now());
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // A concurrent duplicate request already completed this mission period between
            // our exists-check and this insert — the unique constraint caught it. Treat it
            // as the idempotent no-op it actually is, not a 500.
        }
    }

    @Override
    public int totalXpFor(Long userId) {
        return repository.sumXpAwardedByUserId(userId);
    }
}
