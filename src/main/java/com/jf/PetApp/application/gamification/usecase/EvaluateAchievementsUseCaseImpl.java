package com.jf.PetApp.application.gamification.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.achievement.AchievementCatalog;
import com.jf.PetApp.application.gamification.achievement.AchievementContext;
import com.jf.PetApp.application.gamification.achievement.AchievementDefinition;
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

/**
 * Evaluates the real-money-backed {@link AchievementCatalog} against a
 * user's actual portfolio and persists any newly-qualifying achievement.
 * Run on every call (same "recompute live" philosophy as XP/level) — safe
 * to call as often as the client wants, since unlocking is idempotent on
 * {@code (userId, achievementCode)}.
 */
@Service
public class EvaluateAchievementsUseCaseImpl implements EvaluateAchievementsUseCase {

    private final UserRepository userRepository;
    private final AchievementRepositoryPort achievementRepository;
    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;

    public EvaluateAchievementsUseCaseImpl(
            UserRepository userRepository,
            AchievementRepositoryPort achievementRepository,
            GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase,
            GetPortfolioSummaryUseCase getPortfolioSummaryUseCase,
            GetPortfolioAllocationUseCase getPortfolioAllocationUseCase) {
        this.userRepository = userRepository;
        this.achievementRepository = achievementRepository;
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
        this.getPortfolioSummaryUseCase = getPortfolioSummaryUseCase;
        this.getPortfolioAllocationUseCase = getPortfolioAllocationUseCase;
    }

    private static final java.util.Map<InvestmentType, Double> ASSUMED_ANNUAL_YIELD = java.util.Map.of(
            InvestmentType.STOCKS, 0.05,
            InvestmentType.FIXED_INCOME, 0.11,
            InvestmentType.REAL_ESTATE, 0.08,
            InvestmentType.FUNDS, 0.04,
            InvestmentType.CRYPTO, 0.0,
            InvestmentType.OTHERS, 0.0);

    @Override
    @Transactional
    public AchievementEvaluationResult execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long userId = user.getId();

        AchievementContext context = buildContext(userEmail);

        Set<String> newlyUnlocked = new HashSet<>();
        for (AchievementDefinition definition : AchievementCatalog.DEFINITIONS) {
            if (achievementRepository.isUnlocked(userId, definition.code())) {
                continue;
            }
            if (definition.qualifies().test(context)) {
                achievementRepository.unlock(userId, definition.code(), definition.xpReward());
                newlyUnlocked.add(definition.code());
            }
        }

        java.util.Map<String, Instant> unlockedAt = achievementRepository.unlockedWithTimestamps(userId);
        int achievementXpTotal = achievementRepository.totalXpFor(userId);

        return new AchievementEvaluationResult(unlockedAt, newlyUnlocked, achievementXpTotal);
    }

    private AchievementContext buildContext(String userEmail) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(userEmail);
        PortfolioSummaryDTO summary = getPortfolioSummaryUseCase.execute(userEmail);
        List<AllocationSliceDTO> allocation = getPortfolioAllocationUseCase.execute(userEmail);

        boolean hasHoldings = !lots.isEmpty();

        int distinctTypeCount = (int) lots.stream().map(InvestmentLotDTO::type).distinct().count();
        int distinctFundsTickerCount = (int) lots.stream()
                .filter(lot -> lot.type() == InvestmentType.FUNDS)
                .map(InvestmentLotDTO::name)
                .distinct()
                .count();

        java.time.LocalDate firstPurchaseDate = lots.stream()
                .map(InvestmentLotDTO::purchaseDate)
                .min(java.time.LocalDate::compareTo)
                .orElse(null);

        BigDecimal annualPassiveIncome = allocation.stream()
                .map(slice -> slice.currentValue().multiply(BigDecimal.valueOf(ASSUMED_ANNUAL_YIELD.getOrDefault(slice.type(), 0.0))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyPassiveIncome = annualPassiveIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return new AchievementContext(
                hasHoldings,
                summary.currentValue(),
                summary.totalGain(),
                distinctTypeCount,
                distinctFundsTickerCount,
                firstPurchaseDate,
                monthlyPassiveIncome,
                annualPassiveIncome);
    }
}
