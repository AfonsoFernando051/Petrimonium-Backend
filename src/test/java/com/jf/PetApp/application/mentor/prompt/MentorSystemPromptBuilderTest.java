package com.jf.PetApp.application.mentor.prompt;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.mentor.dto.MentorClientContextDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPositionDTO;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentorSystemPromptBuilderTest {

    private Pet petWith(String name, PetSpecieEnum specie) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecie(specie);
        return pet;
    }

    // --- shared behavior (pet name, language resolution) — exercised via buildForWallet,
    // equally applicable to buildForAcademy since both share the same private renderer. ---

    @Test
    void buildForWallet_WithNoPet_UsesGenericPetNamePlaceholder() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "pt");

        assertTrue(prompt.contains("You are your pet,"));
    }

    @Test
    void buildForWallet_WithNamedPet_UsesThePetsRealNameAndSpecie() {
        Pet pet = petWith("Rusty", PetSpecieEnum.FOX);

        String prompt = MentorSystemPromptBuilder.buildForWallet(pet, null, null, null, "pt");

        assertTrue(prompt.contains("You are Rusty,"));
        assertTrue(prompt.contains("- Pet: Rusty (FOX)."));
    }

    @Test
    void buildForWallet_WithPetWithBlankName_FallsBackToGenericPlaceholder() {
        Pet pet = petWith("   ", PetSpecieEnum.DOG);

        String prompt = MentorSystemPromptBuilder.buildForWallet(pet, null, null, null, "pt");

        assertTrue(prompt.contains("You are your pet,"));
    }

    @Test
    void buildForWallet_WithClientLanguage_PrefersClientLanguageOverFallback() {
        MentorClientContextDTO context = new MentorClientContextDTO(null, null, null, "en");

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, context, "pt");

        assertTrue(prompt.contains("Respond naturally and conversationally in en."));
    }

    @Test
    void buildForWallet_WithoutClientLanguage_FallsBackToUsersPreferredLanguage() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "es");

        assertTrue(prompt.contains("Respond naturally and conversationally in es."));
    }

    @Test
    void buildForWallet_WithoutClientLanguageOrFallback_DefaultsToPortuguese() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, null);

        assertTrue(prompt.contains("Respond naturally and conversationally in pt."));
    }

    @Test
    void buildForWallet_WithBlankFallbackLanguage_DefaultsToPortuguese() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "  ");

        assertTrue(prompt.contains("Respond naturally and conversationally in pt."));
    }

    @Test
    void buildForWallet_NeverRecommendsSpecificSecuritiesOrPredictsPrices() {
        // Guards the safety rules block itself stays intact — the prompt's core promise.
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "pt");

        assertTrue(prompt.contains("Never recommend buying or selling a specific security."));
        assertTrue(prompt.contains("Never predict prices or promise returns."));
    }

    @Test
    void buildForWallet_WithClientContextFields_IncludesGoalHorizonAndScreen() {
        MentorClientContextDTO context = new MentorClientContextDTO(
                "Retire early", "10+ years", "portfolio_screen", "en");

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, context, "pt");

        assertTrue(prompt.contains("User's stated investment goal: Retire early"));
        assertTrue(prompt.contains("User's stated investment horizon: 10+ years"));
        assertTrue(prompt.contains("Currently viewing: portfolio_screen"));
    }

    @Test
    void buildForWallet_WithBlankClientContextFields_OmitsThoseLines() {
        MentorClientContextDTO context = new MentorClientContextDTO("  ", null, "", "en");

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, context, "pt");

        assertFalse(prompt.contains("User's stated investment goal"));
        assertFalse(prompt.contains("User's stated investment horizon"));
        assertFalse(prompt.contains("Currently viewing"));
    }

    // --- Wallet: real portfolio only, never Academy content ---

    @Test
    void buildForWallet_WithNoPortfolio_StatesUserHasNoInvestmentsYet() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "pt");

        assertTrue(prompt.contains("the user hasn't registered any investments yet"));
    }

    @Test
    void buildForWallet_WithZeroTotalAssets_TreatedAsNoPortfolio() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, summary, null, null, "pt");

        assertTrue(prompt.contains("the user hasn't registered any investments yet"));
    }

    @Test
    void buildForWallet_WithPortfolio_IncludesRealNumbersInContextBlock() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3);

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, summary, null, null, "pt");

        assertTrue(prompt.contains("Portfolio: 3 asset(s), invested capital 1000.00, current value 1200.00, total gain 200.00 (20.00%)."));
    }

    @Test
    void buildForWallet_WithAllocation_ListsEachSliceWithItsPercentage() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3);
        List<AllocationSliceDTO> allocation = List.of(
                new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(800.0), BigDecimal.valueOf(66.7)),
                new AllocationSliceDTO(InvestmentType.FIXED_INCOME, BigDecimal.valueOf(400.0), BigDecimal.valueOf(33.3))
        );

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, summary, allocation, null, "pt");

        assertTrue(prompt.contains("Allocation by asset type:"));
        assertTrue(prompt.contains("STOCKS: 66.7% of the portfolio"));
        assertTrue(prompt.contains("FIXED_INCOME: 33.3% of the portfolio"));
    }

    @Test
    void buildForWallet_WithEmptyAllocationList_OmitsAllocationSection() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, List.of(), null, "pt");

        assertFalse(prompt.contains("Allocation by asset type:"));
    }

    @Test
    void buildForWallet_NeverMentionsAcademyLessonsOrSimulatedMoney() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3);

        String prompt = MentorSystemPromptBuilder.buildForWallet(null, summary, null, null, "pt");

        assertFalse(prompt.contains("Academy"));
        assertFalse(prompt.contains("lesson"));
        assertFalse(prompt.contains("Simulated"));
        assertFalse(prompt.contains("simulated"));
    }

    @Test
    void buildForWallet_IncludesTheWalletStructuredResponseMarkersAndNotAcademys() {
        String prompt = MentorSystemPromptBuilder.buildForWallet(null, null, null, null, "pt");

        assertTrue(prompt.contains("[[DATA]]"));
        assertTrue(prompt.contains("[[CALCULATION]]"));
        assertTrue(prompt.contains("[[INTERPRETATION]]"));
        assertFalse(prompt.contains("[[CONTENT]]"));
    }

    // --- Academy: simulated portfolio + learning progress only, never real portfolio ---

    @Test
    void buildForAcademy_WithNullSimulatedPortfolio_StatesNoPositionsYet() {
        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", null, null, null);

        assertTrue(prompt.contains("Simulated practice portfolio (virtual money, NOT real"));
        assertTrue(prompt.contains("no positions yet"));
    }

    @Test
    void buildForAcademy_WithPositions_ListsEachTickerAndAlwaysFramesItAsSimulated() {
        SimulatedPortfolioSummaryDTO simulated = new SimulatedPortfolioSummaryDTO(
                BigDecimal.valueOf(9500), BigDecimal.valueOf(10000), "BRL", Instant.now(),
                List.of(new SimulatedPositionDTO("PETR4", BigDecimal.valueOf(10), BigDecimal.valueOf(30), BigDecimal.valueOf(300), BigDecimal.valueOf(100))));

        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, simulated, null, "pt", null, null, null);

        assertTrue(prompt.contains("Simulated practice portfolio (virtual money, NOT real"));
        assertTrue(prompt.contains("Simulated holdings by ticker:"));
        assertTrue(prompt.contains("PETR4:"));
    }

    @Test
    void buildForAcademy_WithNoLearningProgress_OmitsAcademySection() {
        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", null, null, null);

        assertFalse(prompt.contains("Academy progress"));
    }

    @Test
    void buildForAcademy_WithLearningProgress_IncludesLevelXpAndCompletionCounts() {
        LearningProgressResult progress = new LearningProgressResult(
                Set.of("lesson-1", "lesson-2"), Set.of("module-1"), Set.of("lesson-1"), 120, 3, 20, 50);

        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", progress, null, null);

        assertTrue(prompt.contains("Academy progress: level 3 (20/50 XP into this level), 2 lesson(s) completed, 1 module(s) completed."));
    }

    @Test
    void buildForAcademy_WithLearningProgressAndNextLesson_NamesTheLessonAndModule() {
        LearningProgressResult progress = new LearningProgressResult(Set.of(), Set.of(), Set.of(), 0, 1, 0, 50);

        String prompt = MentorSystemPromptBuilder.buildForAcademy(
                null, null, null, "pt", progress, "What is diversification?", "Investing Basics");

        assertTrue(prompt.contains("Next lesson to continue: \"What is diversification?\" (module: Investing Basics)"));
    }

    @Test
    void buildForAcademy_WithLearningProgressAndNoNextLesson_StatesEverythingIsComplete() {
        LearningProgressResult progress = new LearningProgressResult(Set.of(), Set.of(), Set.of(), 0, 1, 0, 50);

        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", progress, null, null);

        assertTrue(prompt.contains("The user has completed every Academy lesson currently available."));
    }

    @Test
    void buildForAcademy_NeverMentionsRealPortfolioNumbers() {
        LearningProgressResult progress = new LearningProgressResult(Set.of(), Set.of(), Set.of(), 0, 1, 0, 50);

        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", progress, null, null);

        assertFalse(prompt.contains("invested capital"));
        assertFalse(prompt.contains("Allocation by asset type"));
    }

    @Test
    void buildForAcademy_IncludesItsOwnMarkersAndNotWalletsThreeWaySplit() {
        String prompt = MentorSystemPromptBuilder.buildForAcademy(null, null, null, "pt", null, null, null);

        assertTrue(prompt.contains("[[CONTENT]]"));
        assertTrue(prompt.contains("[[INTERPRETATION]]"));
        assertFalse(prompt.contains("[[DATA]]"));
        assertFalse(prompt.contains("[[CALCULATION]]"));
    }

    // --- walletSourcesFor: Wallet-only "why am I seeing this?" citation list ---

    @Test
    void walletSourcesFor_WithNothingReal_ReturnsEmptyList() {
        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, null, null);

        assertTrue(sources.isEmpty());
    }

    @Test
    void walletSourcesFor_WithZeroTotalAssets_DoesNotIncludePortfolioSummary() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, summary, null, null);

        assertFalse(sources.contains("portfolio_summary"));
    }

    @Test
    void walletSourcesFor_WithRealPortfolio_IncludesPortfolioSummary() {
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3);

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, summary, null, null);

        assertTrue(sources.contains("portfolio_summary"));
    }

    @Test
    void walletSourcesFor_WithNonEmptyAllocation_IncludesPortfolioAllocation() {
        List<AllocationSliceDTO> allocation = List.of(
                new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(800.0), BigDecimal.valueOf(66.7)));

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, allocation, null);

        assertTrue(sources.contains("portfolio_allocation"));
    }

    @Test
    void walletSourcesFor_WithEmptyAllocation_DoesNotIncludePortfolioAllocation() {
        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, List.of(), null);

        assertFalse(sources.contains("portfolio_allocation"));
    }

    @Test
    void walletSourcesFor_WithPet_IncludesPet() {
        Pet pet = petWith("Rusty", PetSpecieEnum.FOX);

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(pet, null, null, null);

        assertTrue(sources.contains("pet"));
    }

    @Test
    void walletSourcesFor_WithClientGoal_IncludesClientGoal() {
        MentorClientContextDTO context = new MentorClientContextDTO("Retire early", null, null, "en");

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, null, context);

        assertTrue(sources.contains("client_goal"));
    }

    @Test
    void walletSourcesFor_WithClientHorizon_IncludesClientHorizon() {
        MentorClientContextDTO context = new MentorClientContextDTO(null, "10+ years", null, "en");

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, null, context);

        assertTrue(sources.contains("client_horizon"));
    }

    @Test
    void walletSourcesFor_WithClientScreen_IncludesClientScreen() {
        MentorClientContextDTO context = new MentorClientContextDTO(null, null, "portfolio_screen", "en");

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, null, context);

        assertTrue(sources.contains("client_screen"));
    }

    @Test
    void walletSourcesFor_WithBlankClientContextFields_OmitsThoseKeys() {
        MentorClientContextDTO context = new MentorClientContextDTO("  ", null, "", "en");

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(null, null, null, context);

        assertFalse(sources.contains("client_goal"));
        assertFalse(sources.contains("client_horizon"));
        assertFalse(sources.contains("client_screen"));
    }

    @Test
    void walletSourcesFor_WithSeveralRealSourcesAtOnce_IncludesAllOfThem() {
        Pet pet = petWith("Rusty", PetSpecieEnum.FOX);
        PortfolioSummaryDTO summary = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3);
        List<AllocationSliceDTO> allocation = List.of(
                new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(800.0), BigDecimal.valueOf(66.7)));
        MentorClientContextDTO context = new MentorClientContextDTO("Retire early", "10+ years", null, "en");

        List<String> sources = MentorSystemPromptBuilder.walletSourcesFor(pet, summary, allocation, context);

        assertEquals(
                List.of("portfolio_summary", "portfolio_allocation", "pet", "client_goal", "client_horizon"),
                sources);
    }
}
