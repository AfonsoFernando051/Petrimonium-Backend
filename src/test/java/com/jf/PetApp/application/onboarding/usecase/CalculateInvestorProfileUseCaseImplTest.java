package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.ExperienceLevel;
import com.jf.PetApp.core.domain.assessment.FinancialGoal;
import com.jf.PetApp.core.domain.assessment.InvestmentHorizon;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculateInvestorProfileUseCaseImplTest {

    private final CalculateInvestorProfileUseCaseImpl useCase = new CalculateInvestorProfileUseCaseImpl();

    private AcademyOnboardingAnswers answers(FinancialGoal goal, InvestmentHorizon horizon, ExperienceLevel level) {
        return new AcademyOnboardingAnswers(goal, horizon, level);
    }

    @Test
    void allMostConservativeAnswers_ScoresZero_ReturnsGuardian() {
        var result = useCase.execute(
                answers(FinancialGoal.EMERGENCY_FUND, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.NOVICE));

        assertEquals(InvestorProfile.GUARDIAN, result);
    }

    @Test
    void scoreOfTwo_StillReturnsGuardian() {
        // GET_OUT_OF_DEBT(0) + UP_TO_ONE_YEAR(0) + CURIOUS(2) = 2
        var result = useCase.execute(
                answers(FinancialGoal.GET_OUT_OF_DEBT, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.CURIOUS));

        assertEquals(InvestorProfile.GUARDIAN, result);
    }

    @Test
    void scoreOfFour_ReturnsTactician() {
        // GET_OUT_OF_DEBT(0) + ONE_TO_FIVE_YEARS(2) + CURIOUS(2) = 4
        var result = useCase.execute(
                answers(FinancialGoal.GET_OUT_OF_DEBT, InvestmentHorizon.ONE_TO_FIVE_YEARS, ExperienceLevel.CURIOUS));

        assertEquals(InvestorProfile.TACTICIAN, result);
    }

    @Test
    void scoreOfSix_ReturnsAdventurer() {
        // BUY_IMPORTANT_THING(2) + ONE_TO_FIVE_YEARS(2) + CURIOUS(2) = 6
        var result = useCase.execute(
                answers(FinancialGoal.BUY_IMPORTANT_THING, InvestmentHorizon.ONE_TO_FIVE_YEARS, ExperienceLevel.CURIOUS));

        assertEquals(InvestorProfile.ADVENTURER, result);
    }

    @Test
    void allMostAggressiveAnswers_ScoresMax_ReturnsAdventurer() {
        var result = useCase.execute(answers(
                FinancialGoal.INVEST_WITH_CONFIDENCE, InvestmentHorizon.MORE_THAN_FIVE_YEARS, ExperienceLevel.PRACTITIONER));

        assertEquals(InvestorProfile.ADVENTURER, result);
    }

    @Test
    void notSureYetHorizon_ScoresTheSameAsOneToFiveYears() {
        var notSure = useCase.execute(
                answers(FinancialGoal.EMERGENCY_FUND, InvestmentHorizon.NOT_SURE_YET, ExperienceLevel.NOVICE));
        var oneToFive = useCase.execute(
                answers(FinancialGoal.EMERGENCY_FUND, InvestmentHorizon.ONE_TO_FIVE_YEARS, ExperienceLevel.NOVICE));

        assertEquals(oneToFive, notSure);
    }

    @Test
    void justWantToLearnGoal_ScoresTheSameAsBuyImportantThing() {
        var justLearn = useCase.execute(
                answers(FinancialGoal.JUST_WANT_TO_LEARN, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.NOVICE));
        var buyThing = useCase.execute(
                answers(FinancialGoal.BUY_IMPORTANT_THING, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.NOVICE));

        assertEquals(buyThing, justLearn);
    }

    @Test
    void getOutOfDebtGoal_ScoresTheSameAsEmergencyFund() {
        var getOutOfDebt = useCase.execute(
                answers(FinancialGoal.GET_OUT_OF_DEBT, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.NOVICE));
        var emergencyFund = useCase.execute(
                answers(FinancialGoal.EMERGENCY_FUND, InvestmentHorizon.UP_TO_ONE_YEAR, ExperienceLevel.NOVICE));

        assertEquals(emergencyFund, getOutOfDebt);
    }
}
