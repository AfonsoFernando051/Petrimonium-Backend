package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;

public interface CalculateInvestorProfileUseCase {
    InvestorProfile execute(AcademyOnboardingAnswers answers);
}
