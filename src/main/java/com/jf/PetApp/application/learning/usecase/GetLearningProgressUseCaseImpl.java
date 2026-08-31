package com.jf.PetApp.application.learning.usecase;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.gamification.service.LevelCalculator;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.learning.port.LearningCatalogPort;
import com.jf.PetApp.application.learning.port.LessonProgressRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.PlayerLevel;
import com.jf.PetApp.core.domain.learning.LessonProgressSnapshot;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;

@Service
public class GetLearningProgressUseCaseImpl implements GetLearningProgressUseCase {

    private final UserRepository userRepository;
    private final LearningCatalogPort catalogPort;
    private final LessonProgressRepositoryPort progressRepository;
    private final TotalXpCalculator totalXpCalculator;

    public GetLearningProgressUseCaseImpl(
            UserRepository userRepository,
            LearningCatalogPort catalogPort,
            LessonProgressRepositoryPort progressRepository,
            TotalXpCalculator totalXpCalculator) {
        this.userRepository = userRepository;
        this.catalogPort = catalogPort;
        this.progressRepository = progressRepository;
        this.totalXpCalculator = totalXpCalculator;
    }

    @Override
    public LearningProgressResult execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Long userId = user.getId();

        LessonProgressSnapshot progress = progressRepository.snapshotFor(userId);
        Set<String> completedLessonIds = progress.completedLessonIds();
        Set<String> perfectLessonIds = progress.perfectLessonIds();

        Map<String, List<String>> lessonIdsByModule = catalogPort.lessonIdsGroupedByModule();
        Set<String> completedModuleIds = catalogPort.allModules().stream()
                .filter(module -> {
                    List<String> moduleLessonIds = lessonIdsByModule.getOrDefault(module.moduleId(), List.of());
                    return !moduleLessonIds.isEmpty() && completedLessonIds.containsAll(moduleLessonIds);
                })
                .map(ModuleCatalogEntry::moduleId)
                .collect(Collectors.toSet());

        int totalXp = totalXpCalculator.totalXpFor(userId);
        PlayerLevel level = LevelCalculator.fromXp(totalXp);

        return new LearningProgressResult(
                completedLessonIds,
                completedModuleIds,
                perfectLessonIds,
                totalXp,
                level.level(),
                level.xpIntoLevel(),
                level.xpForNextLevel());
    }
}
