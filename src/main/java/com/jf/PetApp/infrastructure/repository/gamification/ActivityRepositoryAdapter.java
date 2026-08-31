package com.jf.PetApp.infrastructure.repository.gamification;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.gamification.port.ActivityRepositoryPort;
import com.jf.PetApp.infrastructure.entity.ActivityLogJpaEntity;

/**
 * The only place in the codebase that knows the activity log is stored as a
 * JPA entity. Implements {@link ActivityRepositoryPort} so
 * {@code StreakService} never touches Spring Data directly.
 */
@Repository
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final ActivityLogJpaRepository repository;

    public ActivityRepositoryAdapter(ActivityLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void recordActivity(Long userId, LocalDate date) {
        if (repository.existsByUserIdAndActivityDate(userId, date)) {
            return;
        }
        ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
        entity.setUserId(userId);
        entity.setActivityDate(date);
        repository.save(entity);
    }

    @Override
    public List<LocalDate> recentActivityDates(Long userId) {
        return repository.findActivityDatesByUserIdOrderByActivityDateDesc(userId);
    }
}
