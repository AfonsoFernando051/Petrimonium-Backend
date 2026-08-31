package com.jf.PetApp.application.lab.usecase;

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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.gamification.service.XpLedgerService;
import com.jf.PetApp.application.lab.dto.SimulatorCompletionResult;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.XpEventType;

class CompleteSimulatorUseCaseImplTest {

    private static final String EMAIL = "learner@test.com";
    private static final Long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    // XpLedgerService is a plain @Service with no interface, so it's exercised
    // for real here (backed by a mocked port) rather than mocked itself —
    // this also verifies its idempotency guarantee end-to-end, which is the
    // whole anti-farming mechanism for simulator XP.
    @Mock
    private XpEventRepositoryPort xpEventRepositoryPort;

    @Mock
    private AchievementRepositoryPort achievementRepositoryPort;

    @Mock
    private MissionRepositoryPort missionRepositoryPort;

    @Mock
    private StreakService streakService;

    private CompleteSimulatorUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        XpLedgerService xpLedgerService = new XpLedgerService(xpEventRepositoryPort);
        TotalXpCalculator totalXpCalculator =
                new TotalXpCalculator(xpLedgerService, achievementRepositoryPort, missionRepositoryPort);
        useCase = new CompleteSimulatorUseCaseImpl(userRepository, xpLedgerService, totalXpCalculator, streakService);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void execute_UnknownUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> useCase.execute("ghost@test.com", "inflation"));
    }

    @Test
    void execute_UnknownSimulatorId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, "not_a_real_simulator"));
    }

    @Test
    void execute_FirstCompletion_GrantsXpAndIsNotAlreadyCompleted() {
        when(xpEventRepositoryPort.existsByUserIdAndEventTypeAndSourceId(
                        USER_ID, XpEventType.SIMULATOR_COMPLETED, "inflation"))
                .thenReturn(false);
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(50);

        SimulatorCompletionResult result = useCase.execute(EMAIL, "inflation");

        assertFalse(result.alreadyCompleted());
        assertEquals(50, result.xpAwarded());
        assertEquals(50, result.totalXp());
        assertEquals("inflation", result.simulatorId());
        verify(xpEventRepositoryPort).save(USER_ID, XpEventType.SIMULATOR_COMPLETED, 50, "inflation");
        verify(streakService).recordActivity(USER_ID);
    }

    // The core anti-farming guarantee: replaying completion for a simulator
    // already granted must never award XP a second time, and totalXp must
    // reflect only the one real grant — not double-count.
    @Test
    void execute_SecondCompletion_GrantsZeroXpAndReportsAlreadyCompleted() {
        when(xpEventRepositoryPort.existsByUserIdAndEventTypeAndSourceId(
                        USER_ID, XpEventType.SIMULATOR_COMPLETED, "inflation"))
                .thenReturn(true);
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(50);

        SimulatorCompletionResult result = useCase.execute(EMAIL, "inflation");

        assertTrue(result.alreadyCompleted());
        assertEquals(0, result.xpAwarded());
        assertEquals(50, result.totalXp());
        verify(xpEventRepositoryPort, never())
                .save(any(), eq(XpEventType.SIMULATOR_COMPLETED), anyInt(), eq("inflation"));
    }

    @Test
    void execute_DifferentSimulators_EachGrantsItsOwnXpIndependently() {
        when(xpEventRepositoryPort.existsByUserIdAndEventTypeAndSourceId(
                        eq(USER_ID), eq(XpEventType.SIMULATOR_COMPLETED), any()))
                .thenReturn(false);
        when(xpEventRepositoryPort.sumAmountByUserId(USER_ID)).thenReturn(50, 100);

        SimulatorCompletionResult first = useCase.execute(EMAIL, "compound_interest");
        SimulatorCompletionResult second = useCase.execute(EMAIL, "diversification");

        assertEquals(50, first.xpAwarded());
        assertEquals(50, second.xpAwarded());
        verify(xpEventRepositoryPort)
                .save(USER_ID, XpEventType.SIMULATOR_COMPLETED, 50, "compound_interest");
        verify(xpEventRepositoryPort)
                .save(USER_ID, XpEventType.SIMULATOR_COMPLETED, 50, "diversification");
    }
}
