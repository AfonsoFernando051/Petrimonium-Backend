package com.jf.PetApp.application.learning.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.gamification.service.XpLedgerService;
import com.jf.PetApp.application.learning.dto.LessonCompletionResult;
import com.jf.PetApp.application.learning.port.LearningCatalogPort;
import com.jf.PetApp.application.learning.port.LessonProgressRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.XpEventType;
import com.jf.PetApp.core.domain.learning.LessonCatalogEntry;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;

class CompleteLessonUseCaseImplTest {

    private static final String EMAIL = "learner@test.com";
    private static final Long USER_ID = 42L;
    private static final String MODULE_ID = "investor_foundations";
    private static final List<String> MODULE_LESSON_IDS = List.of("lesson_1", "lesson_2");

    @Mock
    private UserRepository userRepository;

    @Mock
    private LearningCatalogPort catalogPort;

    @Mock
    private LessonProgressRepositoryPort progressRepository;

    // XpLedgerService is a plain @Service with no interface, so it's exercised
    // for real here (backed by a mocked port) rather than mocked itself —
    // this also verifies its idempotency guarantee end-to-end.
    @Mock
    private XpEventRepositoryPort xpEventRepositoryPort;

    @Mock
    private AchievementRepositoryPort achievementRepositoryPort;

    @Mock
    private MissionRepositoryPort missionRepositoryPort;

    @Mock
    private StreakService streakService;

    private XpLedgerService xpLedgerService;
    private CompleteLessonUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        xpLedgerService = new XpLedgerService(xpEventRepositoryPort);
        TotalXpCalculator totalXpCalculator =
                new TotalXpCalculator(xpLedgerService, achievementRepositoryPort, missionRepositoryPort);
        useCase = new CompleteLessonUseCaseImpl(
                userRepository, catalogPort, progressRepository, xpLedgerService, totalXpCalculator, streakService);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        when(catalogPort.findLesson("lesson_1")).thenReturn(Optional.of(new LessonCatalogEntry("lesson_1", MODULE_ID, 20, 1)));
        when(catalogPort.findModule(MODULE_ID)).thenReturn(Optional.of(new ModuleCatalogEntry(MODULE_ID, 100, 1, 2)));
        when(catalogPort.lessonIdsForModule(MODULE_ID)).thenReturn(MODULE_LESSON_IDS);
    }

    @Test
    void execute_UnknownLesson_ThrowsIllegalArgumentException() {
        when(catalogPort.findLesson("unknown_lesson")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, "unknown_lesson", false));
    }

    @Test
    void execute_FirstCompletion_GrantsLessonXpAndIsNotAlreadyCompleted() {
        when(progressRepository.isLessonCompleted(USER_ID, "lesson_1")).thenReturn(false);
        when(progressRepository.completedLessonIds(USER_ID)).thenReturn(Set.of("lesson_1"));
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(20);

        LessonCompletionResult result = useCase.execute(EMAIL, "lesson_1", false);

        assertFalse(result.alreadyCompleted());
        assertEquals(20, result.xpAwarded());
        assertFalse(result.moduleCompleted());
        assertEquals(20, result.totalXp());
        verify(progressRepository).markCompleted(USER_ID, "lesson_1", false);
        verify(xpEventRepositoryPort).save(USER_ID, XpEventType.LESSON_COMPLETED, 20, "lesson_1");
    }

    @Test
    void execute_PerfectFirstTry_PassesThroughToProgressRepository() {
        when(progressRepository.isLessonCompleted(USER_ID, "lesson_1")).thenReturn(false);
        when(progressRepository.completedLessonIds(USER_ID)).thenReturn(Set.of("lesson_1"));
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(20);

        useCase.execute(EMAIL, "lesson_1", true);

        verify(progressRepository).markCompleted(USER_ID, "lesson_1", true);
    }

    @Test
    void execute_RepeatCompletion_DoesNotDoubleAwardXpButStillRecordsPerfect() {
        when(progressRepository.isLessonCompleted(USER_ID, "lesson_1")).thenReturn(true);
        when(progressRepository.completedLessonIds(USER_ID)).thenReturn(Set.of("lesson_1"));
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(20);

        LessonCompletionResult result = useCase.execute(EMAIL, "lesson_1", true);

        assertTrue(result.alreadyCompleted());
        assertEquals(0, result.xpAwarded());
        // markCompleted is still called on a replay — its own monotonic contract (never
        // downgrading an already-perfect lesson) is what makes this safe, not skipping the call.
        verify(progressRepository).markCompleted(USER_ID, "lesson_1", true);
        verify(xpEventRepositoryPort, never()).save(any(), any(), anyInt(), eq("lesson_1"));
    }

    @Test
    void execute_LastLessonOfModule_AlsoGrantsModuleCompletionXpExactlyOnce() {
        when(progressRepository.isLessonCompleted(USER_ID, "lesson_1")).thenReturn(false);
        // After marking lesson_1 complete, both module lessons are done.
        when(progressRepository.completedLessonIds(USER_ID)).thenReturn(Set.of("lesson_1", "lesson_2"));
        when(xpEventRepositoryPort.existsByUserIdAndEventTypeAndSourceId(USER_ID, XpEventType.MODULE_COMPLETED, MODULE_ID))
                .thenReturn(false);
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(120);

        LessonCompletionResult result = useCase.execute(EMAIL, "lesson_1", false);

        assertTrue(result.moduleCompleted());
        assertEquals(100, result.moduleXpAwarded());
        verify(xpEventRepositoryPort).save(USER_ID, XpEventType.MODULE_COMPLETED, 100, MODULE_ID);
    }

    @Test
    void execute_ModuleAlreadyCompletedBefore_DoesNotReGrantModuleXp() {
        when(progressRepository.isLessonCompleted(USER_ID, "lesson_1")).thenReturn(true);
        when(progressRepository.completedLessonIds(USER_ID)).thenReturn(Set.of("lesson_1", "lesson_2"));
        when(xpEventRepositoryPort.existsByUserIdAndEventTypeAndSourceId(USER_ID, XpEventType.MODULE_COMPLETED, MODULE_ID))
                .thenReturn(true);
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(120);

        LessonCompletionResult result = useCase.execute(EMAIL, "lesson_1", false);

        assertFalse(result.moduleCompleted());
        assertEquals(0, result.moduleXpAwarded());
        verify(xpEventRepositoryPort, never()).save(any(), eq(XpEventType.MODULE_COMPLETED), anyInt(), any());
    }
}
