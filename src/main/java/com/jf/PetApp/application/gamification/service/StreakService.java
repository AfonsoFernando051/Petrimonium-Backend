package com.jf.PetApp.application.gamification.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.gamification.port.ActivityRepositoryPort;
import com.jf.PetApp.core.domain.gamification.StreakSummary;

/**
 * The single place that records engagement activity and derives streak
 * facts from it. Recording is idempotent per (user, day) — logging in
 * twice today never counts as two days of activity.
 */
@Service
public class StreakService {

    private final ActivityRepositoryPort activityRepository;

    public StreakService(ActivityRepositoryPort activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void recordActivity(Long userId) {
        activityRepository.recordActivity(userId, LocalDate.now());
    }

    public StreakSummary currentAndLongestStreak(Long userId) {
        List<LocalDate> datesDesc = activityRepository.recentActivityDates(userId);
        int current = StreakCalculator.currentStreak(datesDesc, LocalDate.now());
        int longest = StreakCalculator.longestStreak(datesDesc);
        return new StreakSummary(current, longest);
    }
}
