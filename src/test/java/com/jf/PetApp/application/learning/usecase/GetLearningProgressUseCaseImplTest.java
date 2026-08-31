package com.jf.PetApp.application.learning.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.gamification.service.XpLedgerService;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.learning.port.LearningCatalogPort;
import com.jf.PetApp.application.learning.port.LessonProgressRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.learning.LessonProgressSnapshot;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;

class GetLearningProgressUseCaseImplTest {

    private static final String EMAIL = "learner@test.com";
    private static final Long USER_ID = 42L;
    private static final String MODULE_ID = "investor_foundations";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LearningCatalogPort catalogPort;

    @Mock
    private LessonProgressRepositoryPort progressRepository;

    @Mock
    private XpEventRepositoryPort xpEventRepositoryPort;

    @Mock
    private AchievementRepositoryPort achievementRepositoryPort;

    @Mock
    private MissionRepositoryPort missionRepositoryPort;

    private GetLearningProgressUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        XpLedgerService xpLedgerService = new XpLedgerService(xpEventRepositoryPort);
        TotalXpCalculator totalXpCalculator =
                new TotalXpCalculator(xpLedgerService, achievementRepositoryPort, missionRepositoryPort);
        useCase = new GetLearningProgressUseCaseImpl(
                userRepository, catalogPort, progressRepository, totalXpCalculator);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(catalogPort.lessonIdsGroupedByModule()).thenReturn(Map.of());
    }

    @Test
    void execute_ModuleFullyCompleted_IsReportedAsCompletedModule() {
        when(progressRepository.snapshotFor(USER_ID))
                .thenReturn(new LessonProgressSnapshot(Set.of("lesson_1", "lesson_2"), Set.of()));
        when(catalogPort.allModules()).thenReturn(List.of(new ModuleCatalogEntry(MODULE_ID, 100, 1, 2)));
        when(catalogPort.lessonIdsGroupedByModule()).thenReturn(Map.of(MODULE_ID, List.of("lesson_1", "lesson_2")));
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(140);

        LearningProgressResult result = useCase.execute(EMAIL);

        assertEquals(Set.of("lesson_1", "lesson_2"), result.completedLessonIds());
        assertTrue(result.completedModuleIds().contains(MODULE_ID));
        assertEquals(140, result.totalXp());
    }

    @Test
    void execute_ModulePartiallyCompleted_IsNotReportedAsCompletedModule() {
        when(progressRepository.snapshotFor(USER_ID))
                .thenReturn(new LessonProgressSnapshot(Set.of("lesson_1"), Set.of()));
        when(catalogPort.allModules()).thenReturn(List.of(new ModuleCatalogEntry(MODULE_ID, 100, 1, 2)));
        when(catalogPort.lessonIdsGroupedByModule()).thenReturn(Map.of(MODULE_ID, List.of("lesson_1", "lesson_2")));
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(20);

        LearningProgressResult result = useCase.execute(EMAIL);

        assertTrue(result.completedModuleIds().isEmpty());
    }

    @Test
    void execute_ExposesPerfectLessonIdsFromProgressRepository() {
        when(progressRepository.snapshotFor(USER_ID))
                .thenReturn(new LessonProgressSnapshot(Set.of("lesson_1"), Set.of("lesson_1")));
        when(catalogPort.allModules()).thenReturn(List.of());
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(20);

        LearningProgressResult result = useCase.execute(EMAIL);

        assertEquals(Set.of("lesson_1"), result.perfectLessonIds());
    }
}
