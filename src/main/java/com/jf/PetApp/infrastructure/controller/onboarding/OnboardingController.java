package com.jf.PetApp.infrastructure.controller.onboarding;

import com.jf.PetApp.application.onboarding.usecase.SubmitAssessmentUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.ExperienceLevel;
import com.jf.PetApp.core.domain.assessment.FinancialGoal;
import com.jf.PetApp.core.domain.assessment.InvestmentHorizon;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final SubmitAssessmentUseCase submitAssessmentUseCase;
    private final UserRepository userRepository;

    public OnboardingController(
            SubmitAssessmentUseCase submitAssessmentUseCase,
            UserRepository userRepository) {
        this.submitAssessmentUseCase = submitAssessmentUseCase;
        this.userRepository = userRepository;
    }

    @PostMapping("/submit")
    public ResponseEntity<ProfileResponseDTO> submitAssessment(@RequestBody SubmitAssessmentRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        AcademyOnboardingAnswers answers =
                new AcademyOnboardingAnswers(request.goal(), request.investmentHorizon(), request.experienceLevel());
        InvestorProfile profile = submitAssessmentUseCase.execute(email, answers);
        return ResponseEntity.ok(new ProfileResponseDTO(profile.name()));
    }

    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusDTO> getStatus() {
        User user = resolveCurrentUser();
        String profile = user.getInvestorProfile() == null ? null : user.getInvestorProfile().name();
        return ResponseEntity.ok(new OnboardingStatusDTO(user.hasAnsweredOnboarding(), profile));
    }

    private User resolveCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public record SubmitAssessmentRequestDTO(
            FinancialGoal goal,
            InvestmentHorizon investmentHorizon,
            ExperienceLevel experienceLevel) {}
    public record ProfileResponseDTO(String profile) {}
    public record OnboardingStatusDTO(boolean hasAnswered, String profile) {}
}
