package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import org.springframework.stereotype.Service;

@Service
public class SubmitAssessmentUseCaseImpl implements SubmitAssessmentUseCase {

    private final UserRepository userRepository;
    private final CalculateInvestorProfileUseCase calculateInvestorProfileUseCase;

    public SubmitAssessmentUseCaseImpl(UserRepository userRepository,
                                        CalculateInvestorProfileUseCase calculateInvestorProfileUseCase) {
        this.userRepository = userRepository;
        this.calculateInvestorProfileUseCase = calculateInvestorProfileUseCase;
    }

    @Override
    public InvestorProfile execute(String email, AcademyOnboardingAnswers answers) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        // Already completed onboarding: return the existing profile instead of
        // recomputing, so a retried/duplicate submit is idempotent.
        if (user.hasAnsweredOnboarding() && user.getInvestorProfile() != null) {
            return user.getInvestorProfile();
        }

        InvestorProfile profile = calculateInvestorProfileUseCase.execute(answers);

        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(profile);
        userRepository.save(user);

        return profile;
    }
}
