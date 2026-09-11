package com.jf.PetApp.core.domain.assessment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain record with no validation annotations or custom mapping logic --
 * construction/accessor test per this batch's exhaustive-coverage scope.
 */
class AcademyOnboardingAnswersTest {

    @Test
    void accessorsReturnConstructedValues() {
        AcademyOnboardingAnswers answers = new AcademyOnboardingAnswers(
                FinancialGoal.INVEST_WITH_CONFIDENCE, InvestmentHorizon.MORE_THAN_FIVE_YEARS, ExperienceLevel.PRACTITIONER);

        assertEquals(FinancialGoal.INVEST_WITH_CONFIDENCE, answers.goal());
        assertEquals(InvestmentHorizon.MORE_THAN_FIVE_YEARS, answers.investmentHorizon());
        assertEquals(ExperienceLevel.PRACTITIONER, answers.experienceLevel());
    }
}
