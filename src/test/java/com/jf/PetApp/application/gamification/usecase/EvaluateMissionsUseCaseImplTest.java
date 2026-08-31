package com.jf.PetApp.application.gamification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.dto.MissionEvaluationResult;
import com.jf.PetApp.application.gamification.dto.MissionStatusDTO;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.XpEventType;

class EvaluateMissionsUseCaseImplTest {

    private static final String EMAIL = "learner@test.com";
    private static final Long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MissionRepositoryPort missionRepository;

    @Mock
    private XpEventRepositoryPort xpEventRepository;

    private EvaluateMissionsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new EvaluateMissionsUseCaseImpl(userRepository, missionRepository, xpEventRepository);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void execute_NoLearningActivityThisPeriod_CompletesNothing() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), any(), any(), any())).thenReturn(0);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        assertTrue(result.newlyCompletedCodes().isEmpty());
        assertTrue(result.missions().stream().noneMatch(MissionStatusDTO::completed));
        verify(missionRepository, never()).complete(any(), any(), any(), anyInt());
    }

    @Test
    void execute_OneLessonCompletedToday_CompletesTheOneLessonDailyMissionOnly() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.LESSON_COMPLETED), any(), any())).thenReturn(1);
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.MODULE_COMPLETED), any(), any())).thenReturn(0);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        assertTrue(result.newlyCompletedCodes().contains("daily_complete_lesson"));
        assertFalse(result.newlyCompletedCodes().contains("daily_complete_two_lessons"));
        assertFalse(result.newlyCompletedCodes().contains("weekly_complete_three_lessons"));
        verify(missionRepository).complete(eq(USER_ID), eq("daily_complete_lesson"), any(), eq(30));
        verify(missionRepository, never()).complete(eq(USER_ID), eq("daily_complete_two_lessons"), any(), anyInt());
    }

    @Test
    void execute_ThreeLessonsCompletedThisWeek_CompletesTheWeeklyLessonMission() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.LESSON_COMPLETED), any(), any())).thenReturn(3);
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.MODULE_COMPLETED), any(), any())).thenReturn(0);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        assertTrue(result.newlyCompletedCodes().contains("weekly_complete_three_lessons"));
        verify(missionRepository).complete(eq(USER_ID), eq("weekly_complete_three_lessons"), any(), eq(100));
    }

    @Test
    void execute_AlreadyCompletedThisPeriod_NeverReCompletesOrReGrantsXp() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.LESSON_COMPLETED), any(), any())).thenReturn(5);
        when(missionRepository.isCompleted(eq(USER_ID), eq("daily_complete_lesson"), any())).thenReturn(true);
        when(missionRepository.isCompleted(eq(USER_ID), eq("daily_complete_two_lessons"), any())).thenReturn(true);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        assertFalse(result.newlyCompletedCodes().contains("daily_complete_lesson"));
        assertFalse(result.newlyCompletedCodes().contains("daily_complete_two_lessons"));
        verify(missionRepository, never()).complete(eq(USER_ID), eq("daily_complete_lesson"), any(), anyInt());
    }

    @Test
    void execute_ProgressIsCappedAtTarget_NeverReportedAboveIt() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.LESSON_COMPLETED), any(), any())).thenReturn(50);
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), eq(XpEventType.MODULE_COMPLETED), any(), any())).thenReturn(0);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        MissionStatusDTO dailyOne = result.missions().stream()
                .filter(m -> m.code().equals("daily_complete_lesson")).findFirst().orElseThrow();
        assertEquals(1, dailyOne.progress());
        assertEquals(1, dailyOne.target());
    }

    @Test
    void execute_ReturnsRealTotalMissionXpFromRepository() {
        when(xpEventRepository.countByUserIdAndEventTypeAndCreatedAtBetween(
                eq(USER_ID), any(), any(), any())).thenReturn(0);
        when(missionRepository.totalXpFor(USER_ID)).thenReturn(190);

        MissionEvaluationResult result = useCase.execute(EMAIL);

        assertEquals(190, result.missionXpTotal());
    }

    @Test
    void execute_UnknownUser_ThrowsRatherThanEvaluatingBlindly() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> useCase.execute("ghost@test.com"));
    }
}
