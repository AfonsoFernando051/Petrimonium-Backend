package com.jf.PetApp.infrastructure.controller.onboarding;

import com.jf.PetApp.application.onboarding.usecase.SubmitAssessmentUseCase;
import com.jf.PetApp.application.translation.service.TranslationCacheService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.port.QuestionRepository;
import com.jf.PetApp.core.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final QuestionRepository questionRepository;
    private final SubmitAssessmentUseCase submitAssessmentUseCase;
    private final UserRepository userRepository;
    private final TranslationCacheService translationCacheService;

    public OnboardingController(
            QuestionRepository questionRepository,
            SubmitAssessmentUseCase submitAssessmentUseCase,
            UserRepository userRepository,
            TranslationCacheService translationCacheService) {
        this.questionRepository = questionRepository;
        this.submitAssessmentUseCase = submitAssessmentUseCase;
        this.userRepository = userRepository;
        this.translationCacheService = translationCacheService;
    }

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionResponseDTO>> getQuestions(
            @RequestParam(defaultValue = TranslationCacheService.SOURCE_LANGUAGE) String lang) {
        List<QuestionResponseDTO> responses = questionRepository.findAll().stream()
                .map(q -> new QuestionResponseDTO(
                        q.id(),
                        translationCacheService.translate(q.text(), lang),
                        q.options().stream()
                                .map(o -> new OptionResponseDTO(o.id(), translationCacheService.translate(o.text(), lang)))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/submit")
    public ResponseEntity<ProfileResponseDTO> submitAssessment(@RequestBody SubmitAssessmentRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        InvestorProfile profile = submitAssessmentUseCase.execute(email, request.selectedOptionIds());
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

    public record OptionResponseDTO(String id, String text) {}
    public record QuestionResponseDTO(String id, String text, List<OptionResponseDTO> options) {}
    public record SubmitAssessmentRequestDTO(List<String> selectedOptionIds) {}
    public record ProfileResponseDTO(String profile) {}
    public record OnboardingStatusDTO(boolean hasAnswered, String profile) {}
}
