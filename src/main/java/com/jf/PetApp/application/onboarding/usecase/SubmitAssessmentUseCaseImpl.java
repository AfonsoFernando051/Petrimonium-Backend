package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.assessment.UserAssessment;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public InvestorProfile execute(String email, List<String> selectedOptionIds) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        // Already completed onboarding: return the existing profile instead of
        // recomputing, so a retried/duplicate submit is idempotent.
        if (user.hasAnsweredOnboarding() && user.getInvestorProfile() != null) {
            return user.getInvestorProfile();
        }

        UserAssessment assessment = new UserAssessment(user.getId(), selectedOptionIds);
        InvestorProfile profile = calculateInvestorProfileUseCase.execute(assessment);

        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(profile);
        userRepository.save(user);

        return profile;
    }
}
