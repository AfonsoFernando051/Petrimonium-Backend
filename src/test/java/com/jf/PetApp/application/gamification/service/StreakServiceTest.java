package com.jf.PetApp.application.gamification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jf.PetApp.application.gamification.port.ActivityRepositoryPort;
import com.jf.PetApp.core.domain.gamification.StreakSummary;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private ActivityRepositoryPort activityRepository;

    private StreakService streakService;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        streakService = new StreakService(activityRepository);
    }

    @Test
    void recordActivity_DelegatesToRepositoryWithTodaysDate() {
        streakService.recordActivity(USER_ID);

        verify(activityRepository).recordActivity(USER_ID, LocalDate.now());
    }

    @Test
    void currentAndLongestStreak_NoActivity_ReturnsZeroForBoth() {
        when(activityRepository.recentActivityDates(USER_ID)).thenReturn(List.of());

        StreakSummary summary = streakService.currentAndLongestStreak(USER_ID);

        assertEquals(0, summary.currentStreak());
        assertEquals(0, summary.longestStreak());
    }

    @Test
    void currentAndLongestStreak_ActiveToday_ComputesCurrentAndLongestFromDates() {
        LocalDate today = LocalDate.now();
        List<LocalDate> datesDesc = List.of(today, today.minusDays(1), today.minusDays(2));
        when(activityRepository.recentActivityDates(USER_ID)).thenReturn(datesDesc);

        StreakSummary summary = streakService.currentAndLongestStreak(USER_ID);

        assertEquals(3, summary.currentStreak());
        assertEquals(3, summary.longestStreak());
    }

    @Test
    void currentAndLongestStreak_GapBeforeYesterday_CurrentIsZeroButLongestReflectsPastRun() {
        LocalDate today = LocalDate.now();
        List<LocalDate> datesDesc = List.of(
                today.minusDays(5), today.minusDays(6), today.minusDays(7));
        when(activityRepository.recentActivityDates(USER_ID)).thenReturn(datesDesc);

        StreakSummary summary = streakService.currentAndLongestStreak(USER_ID);

        assertEquals(0, summary.currentStreak());
        assertEquals(3, summary.longestStreak());
    }
}
