package com.jf.PetApp.application.gamification.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.dto.MissionEvaluationResult;
import com.jf.PetApp.application.gamification.dto.MissionStatusDTO;
import com.jf.PetApp.application.gamification.mission.MissionCatalog;
import com.jf.PetApp.application.gamification.mission.MissionContext;
import com.jf.PetApp.application.gamification.mission.MissionDefinition;
import com.jf.PetApp.application.gamification.mission.MissionPeriodKeyCalculator;
import com.jf.PetApp.application.gamification.mission.MissionPeriodKeyCalculator.PeriodWindow;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.XpEventType;

/**
 * Evaluates the fixed, learning-only {@link MissionCatalog} against a
 * user's real completion history in the XP ledger and persists any
 * newly-completed mission instance. Run on every call (same "recompute
 * live" philosophy as achievements) — safe to call as often as the client
 * wants, since completion is idempotent on
 * {@code (userId, missionCode, periodKey)}.
 */
@Service
public class EvaluateMissionsUseCaseImpl implements EvaluateMissionsUseCase {

    private final UserRepository userRepository;
    private final MissionRepositoryPort missionRepository;
    private final XpEventRepositoryPort xpEventRepository;

    public EvaluateMissionsUseCaseImpl(
            UserRepository userRepository,
            MissionRepositoryPort missionRepository,
            XpEventRepositoryPort xpEventRepository) {
        this.userRepository = userRepository;
        this.missionRepository = missionRepository;
        this.xpEventRepository = xpEventRepository;
    }

    @Override
    @Transactional
    public MissionEvaluationResult execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        List<MissionStatusDTO> statuses = new ArrayList<>();
        Set<String> newlyCompleted = new HashSet<>();

        for (MissionDefinition definition : MissionCatalog.DEFINITIONS) {
            PeriodWindow window = MissionPeriodKeyCalculator.windowFor(definition.period(), today);
            MissionContext context = buildContext(userId, window);

            boolean completed = missionRepository.isCompleted(userId, definition.code(), window.periodKey());
            if (!completed && definition.isComplete(context)) {
                missionRepository.complete(userId, definition.code(), window.periodKey(), definition.xpReward());
                completed = true;
                newlyCompleted.add(definition.code());
            }

            statuses.add(new MissionStatusDTO(
                    definition.code(),
                    definition.period(),
                    window.periodKey(),
                    definition.progressFor(context),
                    definition.targetCount(),
                    definition.xpReward(),
                    completed));
        }

        int missionXpTotal = missionRepository.totalXpFor(userId);
        return new MissionEvaluationResult(statuses, newlyCompleted, missionXpTotal);
    }

    private MissionContext buildContext(Long userId, PeriodWindow window) {
        int lessonsCompleted = xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                userId, XpEventType.LESSON_COMPLETED, window.startInclusive(), window.endExclusive());
        int modulesCompleted = xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                userId, XpEventType.MODULE_COMPLETED, window.startInclusive(), window.endExclusive());
        return new MissionContext(lessonsCompleted, modulesCompleted);
    }
}
