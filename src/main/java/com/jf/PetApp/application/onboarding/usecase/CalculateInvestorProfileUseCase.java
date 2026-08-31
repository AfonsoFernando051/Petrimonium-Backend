package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.assessment.UserAssessment;

public interface CalculateInvestorProfileUseCase {
    InvestorProfile execute(UserAssessment assessment);
}
