package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.ExperienceLevel;
import com.jf.PetApp.core.domain.assessment.FinancialGoal;
import com.jf.PetApp.core.domain.assessment.InvestmentHorizon;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import org.springframework.stereotype.Service;

/**
 * Classifies a user's real Academy-onboarding answers (goal, investment horizon,
 * experience level — already gathered by {@code FinancialGoalScreen}/
 * {@code TimeHorizonScreen}/{@code ExperienceLevelScreen}) into an
 * {@link InvestorProfile}.
 *
 * <p>Replaces the earlier five-question risk-tolerance survey, which lived only
 * in a repository no client UI ever reached — this scores the three signals
 * Academy's real onboarding already collects instead of a separate
 * questionnaire nobody answers.</p>
 *
 * <p>Each answer scores 0/2/4, mirroring the old survey's per-option scale; the
 * three-answer max of 12 keeps roughly the same tier proportions as the old
 * five-question max of 20 (a third of the range each).</p>
 */
@Service
public class CalculateInvestorProfileUseCaseImpl implements CalculateInvestorProfileUseCase {

    @Override
    public InvestorProfile execute(AcademyOnboardingAnswers answers) {
        int score = scoreGoal(answers.goal())
                + scoreHorizon(answers.investmentHorizon())
                + scoreExperience(answers.experienceLevel());

        if (score <= 2) return InvestorProfile.GUARDIAN;
        if (score <= 5) return InvestorProfile.TACTICIAN;
        return InvestorProfile.ADVENTURER;
    }

    private int scoreGoal(FinancialGoal goal) {
        return switch (goal) {
            case EMERGENCY_FUND, GET_OUT_OF_DEBT -> 0;
            case BUY_IMPORTANT_THING, JUST_WANT_TO_LEARN -> 2;
            case INVEST_WITH_CONFIDENCE -> 4;
        };
    }

    private int scoreHorizon(InvestmentHorizon horizon) {
        return switch (horizon) {
            case UP_TO_ONE_YEAR -> 0;
            case ONE_TO_FIVE_YEARS, NOT_SURE_YET -> 2;
            case MORE_THAN_FIVE_YEARS -> 4;
        };
    }

    private int scoreExperience(ExperienceLevel level) {
        return switch (level) {
            case NOVICE -> 0;
            case CURIOUS -> 2;
            case PRACTITIONER -> 4;
        };
    }
}
