package com.jf.PetApp.application.gamification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.jf.PetApp.application.gamification.dto.AchievementEvaluationResult;
import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;
import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.investment.usecase.GetPortfolioAllocationUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioHoldingsUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioSummaryUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.InvestmentType;

class EvaluateAchievementsUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";
    private static final Long USER_ID = 7L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AchievementRepositoryPort achievementRepository;

    @Mock
    private GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    @Mock
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    @Mock
    private GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;

    private EvaluateAchievementsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new EvaluateAchievementsUseCaseImpl(
                userRepository, achievementRepository,
                getPortfolioHoldingsUseCase, getPortfolioSummaryUseCase, getPortfolioAllocationUseCase);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void execute_EmptyPortfolio_UnlocksNothing() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of());
        when(getPortfolioSummaryUseCase.execute(EMAIL)).thenReturn(new PortfolioSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0));
        when(getPortfolioAllocationUseCase.execute(EMAIL)).thenReturn(List.of());
        when(achievementRepository.unlockedWithTimestamps(USER_ID)).thenReturn(Map.of());

        AchievementEvaluationResult result = useCase.execute(EMAIL);

        assertTrue(result.newlyUnlockedCodes().isEmpty());
        verify(achievementRepository, never()).unlock(eq(USER_ID), org.mockito.ArgumentMatchers.anyString(), anyInt());
    }

    @Test
    void execute_HasHoldings_UnlocksFirstInvestmentExactlyOnce() {
        InvestmentLotDTO lot = new InvestmentLotDTO(
                1, "PETR4", InvestmentType.STOCKS, BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0), LocalDate.now(),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(300.0), BigDecimal.valueOf(300.0));
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot));
        when(getPortfolioSummaryUseCase.execute(EMAIL)).thenReturn(new PortfolioSummaryDTO(
                BigDecimal.valueOf(300.0), BigDecimal.valueOf(300.0), BigDecimal.ZERO, BigDecimal.ZERO, 1));
        when(getPortfolioAllocationUseCase.execute(EMAIL))
                .thenReturn(List.of(new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(300.0), BigDecimal.valueOf(100.0))));
        when(achievementRepository.isUnlocked(USER_ID, "first_investment")).thenReturn(false);
        when(achievementRepository.unlockedWithTimestamps(USER_ID)).thenReturn(Map.of());

        AchievementEvaluationResult result = useCase.execute(EMAIL);

        assertTrue(result.newlyUnlockedCodes().contains("first_investment"));
        // DECISION-027: first_investment is investment-activity-derived, so it's a 0-XP
        // milestone — same treatment as the wealth/profit-derived achievements (DECISION-014).
        verify(achievementRepository).unlock(USER_ID, "first_investment", 0);
    }

    @Test
    void execute_AlreadyUnlocked_NeverReGrantsXp() {
        InvestmentLotDTO lot = new InvestmentLotDTO(
                1, "PETR4", InvestmentType.STOCKS, BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0), LocalDate.now(),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(300.0), BigDecimal.valueOf(300.0));
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot));
        when(getPortfolioSummaryUseCase.execute(EMAIL)).thenReturn(new PortfolioSummaryDTO(
                BigDecimal.valueOf(300.0), BigDecimal.valueOf(300.0), BigDecimal.ZERO, BigDecimal.ZERO, 1));
        when(getPortfolioAllocationUseCase.execute(EMAIL))
                .thenReturn(List.of(new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(300.0), BigDecimal.valueOf(100.0))));
        when(achievementRepository.isUnlocked(USER_ID, "first_investment")).thenReturn(true);
        when(achievementRepository.unlockedWithTimestamps(USER_ID)).thenReturn(Map.of());

        AchievementEvaluationResult result = useCase.execute(EMAIL);

        assertFalse(result.newlyUnlockedCodes().contains("first_investment"));
        verify(achievementRepository, never()).unlock(eq(USER_ID), eq("first_investment"), anyInt());
    }

    @Test
    void execute_ReturnsRealTotalXpFromRepository() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of());
        when(getPortfolioSummaryUseCase.execute(EMAIL)).thenReturn(new PortfolioSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0));
        when(getPortfolioAllocationUseCase.execute(EMAIL)).thenReturn(List.of());
        when(achievementRepository.unlockedWithTimestamps(USER_ID)).thenReturn(Map.of());
        when(achievementRepository.totalXpFor(USER_ID)).thenReturn(50);

        AchievementEvaluationResult result = useCase.execute(EMAIL);

        assertEquals(50, result.achievementXpTotal());
    }
}
