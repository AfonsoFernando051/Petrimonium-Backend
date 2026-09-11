package com.jf.PetApp.core.domain.assessment;

/**
 * The three real signals Academy's onboarding already collects — goal
 * ({@code FinancialGoalScreen}), horizon ({@code TimeHorizonScreen}) and
 * self-reported experience ({@code ExperienceLevelScreen}) — submitted once
 * all three are known so {@link CalculateInvestorProfileUseCase} can classify
 * an {@link InvestorProfile} from real onboarding answers instead of a
 * separate questionnaire no client UI ever reached.
 */
public record AcademyOnboardingAnswers(
    FinancialGoal goal,
    InvestmentHorizon investmentHorizon,
    ExperienceLevel experienceLevel
) {}
