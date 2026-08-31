package com.jf.PetApp.application.gamification.service;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;

/**
 * The single place that combines every XP source into one number — the
 * "single source" this codebase's docs (see {@code ACADEMY_ENGINE.md} §7)
 * anticipated under this exact name. XP currently lives in three separate
 * tables ({@code xp_events}, {@code achievement_unlocks},
 * {@code mission_completions}); this is where they're summed, so a new XP
 * source only ever needs wiring in one place instead of at every call site
 * that used to add {@code xpLedgerService.totalXpFor(userId) +
 * achievementRepository.totalXpFor(userId)} by hand.
 */
@Service
public class TotalXpCalculator {

    private final XpLedgerService xpLedgerService;
    private final AchievementRepositoryPort achievementRepository;
    private final MissionRepositoryPort missionRepository;

    public TotalXpCalculator(
            XpLedgerService xpLedgerService,
            AchievementRepositoryPort achievementRepository,
            MissionRepositoryPort missionRepository) {
        this.xpLedgerService = xpLedgerService;
        this.achievementRepository = achievementRepository;
        this.missionRepository = missionRepository;
    }

    public int totalXpFor(Long userId) {
        return xpLedgerService.totalXpFor(userId)
                + achievementRepository.totalXpFor(userId)
                + missionRepository.totalXpFor(userId);
    }
}
