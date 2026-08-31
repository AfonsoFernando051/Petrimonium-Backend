package com.jf.PetApp.application.lab.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.gamification.service.XpLedgerService;
import com.jf.PetApp.application.gamification.service.LevelCalculator;
import com.jf.PetApp.application.lab.dto.SimulatorCompletionResult;
import com.jf.PetApp.application.lab.simulator.SimulatorCatalog;
import com.jf.PetApp.application.lab.simulator.SimulatorCatalog.SimulatorDefinition;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.PlayerLevel;
import com.jf.PetApp.core.domain.gamification.XpEventType;

/**
 * Records a Financial Lab simulator completion and grants its XP
 * (DECISION-037). There is no separate progress table — the {@code
 * xp_events} row for {@code SIMULATOR_COMPLETED} *is* the completion
 * record, so {@link XpLedgerService#grantXp}'s own idempotency (on
 * {@code (userId, eventType, sourceId)}) is both the completion check and
 * the anti-farming guard: replaying this call for an already-completed
 * simulator is always safe and simply reports zero XP awarded, matching
 * {@code CompleteLessonUseCaseImpl}'s contract.
 */
@Service
public class CompleteSimulatorUseCaseImpl implements CompleteSimulatorUseCase {

    private final UserRepository userRepository;
    private final XpLedgerService xpLedgerService;
    private final TotalXpCalculator totalXpCalculator;
    private final StreakService streakService;

    public CompleteSimulatorUseCaseImpl(
            UserRepository userRepository,
            XpLedgerService xpLedgerService,
            TotalXpCalculator totalXpCalculator,
            StreakService streakService) {
        this.userRepository = userRepository;
        this.xpLedgerService = xpLedgerService;
        this.totalXpCalculator = totalXpCalculator;
        this.streakService = streakService;
    }

    @Override
    @Transactional
    public SimulatorCompletionResult execute(String userEmail, String simulatorId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long userId = user.getId();

        SimulatorDefinition definition = SimulatorCatalog.find(simulatorId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown simulator id: " + simulatorId));

        boolean granted = xpLedgerService.grantXp(
                userId, XpEventType.SIMULATOR_COMPLETED, definition.xpReward(), simulatorId);
        boolean alreadyCompleted = !granted;
        int xpAwarded = granted ? definition.xpReward() : 0;

        streakService.recordActivity(userId);

        int totalXp = totalXpCalculator.totalXpFor(userId);
        PlayerLevel level = LevelCalculator.fromXp(totalXp);

        return new SimulatorCompletionResult(
                simulatorId,
                alreadyCompleted,
                xpAwarded,
                totalXp,
                level.level(),
                level.xpIntoLevel(),
                level.xpForNextLevel());
    }
}
