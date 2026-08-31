package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;

import java.util.List;

public interface SubmitAssessmentUseCase {
    InvestorProfile execute(String email, List<String> selectedOptionIds);
}
