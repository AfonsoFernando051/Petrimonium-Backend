package com.jf.PetApp.application.gamification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.dto.GamificationSummaryResult;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.StreakSummary;

@ExtendWith(MockitoExtension.class)
class GetGamificationSummaryUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotalXpCalculator totalXpCalculator;

    @Mock
    private StreakService streakService;

    private GetGamificationSummaryUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        useCase = new GetGamificationSummaryUseCaseImpl(userRepository, totalXpCalculator, streakService);
    }

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        return user;
    }

    @Test
    void execute_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL));
    }

    @Test
    void execute_UserAtLevelOneWithNoXp_ReturnsZeroedLevelFields() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithId(USER_ID)));
        when(totalXpCalculator.totalXpFor(USER_ID)).thenReturn(0);
        when(streakService.currentAndLongestStreak(USER_ID)).thenReturn(new StreakSummary(0, 0));

        GamificationSummaryResult result = useCase.execute(EMAIL);

        assertEquals(0, result.totalXp());
        assertEquals(1, result.level());
        assertEquals(0, result.xpIntoLevel());
        assertEquals(50, result.xpForNextLevel());
        assertEquals(0, result.currentStreak());
        assertEquals(0, result.longestStreak());
    }

    @Test
    void execute_UserWithXpAndStreak_ComputesLevelAndCarriesStreakThrough() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithId(USER_ID)));
        // 50 XP is exactly the threshold for level 2 (totalXpForLevel(2) = 50*1*2/2 = 50).
        when(totalXpCalculator.totalXpFor(USER_ID)).thenReturn(50);
        when(streakService.currentAndLongestStreak(USER_ID)).thenReturn(new StreakSummary(3, 10));

        GamificationSummaryResult result = useCase.execute(EMAIL);

        assertEquals(50, result.totalXp());
        assertEquals(2, result.level());
        assertEquals(0, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
        assertEquals(3, result.currentStreak());
        assertEquals(10, result.longestStreak());
    }

    @Test
    void execute_PassesUserIdFromLookedUpUserToCollaborators() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithId(USER_ID)));
        when(totalXpCalculator.totalXpFor(USER_ID)).thenReturn(10);
        when(streakService.currentAndLongestStreak(USER_ID)).thenReturn(new StreakSummary(1, 1));

        useCase.execute(EMAIL);

        org.mockito.Mockito.verify(totalXpCalculator).totalXpFor(USER_ID);
        org.mockito.Mockito.verify(streakService).currentAndLongestStreak(USER_ID);
    }
}
