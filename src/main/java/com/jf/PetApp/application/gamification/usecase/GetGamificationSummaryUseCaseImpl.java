package com.jf.PetApp.application.gamification.usecase;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.dto.GamificationSummaryResult;
import com.jf.PetApp.application.gamification.service.LevelCalculator;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.PlayerLevel;
import com.jf.PetApp.core.domain.gamification.StreakSummary;

@Service
public class GetGamificationSummaryUseCaseImpl implements GetGamificationSummaryUseCase {

    private final UserRepository userRepository;
    private final TotalXpCalculator totalXpCalculator;
    private final StreakService streakService;

    public GetGamificationSummaryUseCaseImpl(
            UserRepository userRepository,
            TotalXpCalculator totalXpCalculator,
            StreakService streakService) {
        this.userRepository = userRepository;
        this.totalXpCalculator = totalXpCalculator;
        this.streakService = streakService;
    }

    @Override
    public GamificationSummaryResult execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long userId = user.getId();

        int totalXp = totalXpCalculator.totalXpFor(userId);
        PlayerLevel level = LevelCalculator.fromXp(totalXp);
        StreakSummary streak = streakService.currentAndLongestStreak(userId);

        return new GamificationSummaryResult(
                totalXp, level.level(), level.xpIntoLevel(), level.xpForNextLevel(),
                streak.currentStreak(), streak.longestStreak());
    }
}
