package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.AcademyOnboardingAnswers;
import com.jf.PetApp.core.domain.assessment.ExperienceLevel;
import com.jf.PetApp.core.domain.assessment.FinancialGoal;
import com.jf.PetApp.core.domain.assessment.InvestmentHorizon;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubmitAssessmentUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CalculateInvestorProfileUseCase calculateInvestorProfileUseCase;

    @InjectMocks
    private SubmitAssessmentUseCaseImpl submitAssessmentUseCase;

    private static final AcademyOnboardingAnswers ANSWERS = new AcademyOnboardingAnswers(
            FinancialGoal.INVEST_WITH_CONFIDENCE, InvestmentHorizon.MORE_THAN_FIVE_YEARS, ExperienceLevel.PRACTITIONER);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenUserHasNotAnsweredOnboarding_ShouldCalculateAndPersistProfile() {
        String email = "investor@test.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setHasAnsweredOnboarding(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(calculateInvestorProfileUseCase.execute(any(AcademyOnboardingAnswers.class))).thenReturn(InvestorProfile.TACTICIAN);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvestorProfile result = submitAssessmentUseCase.execute(email, ANSWERS);

        assertEquals(InvestorProfile.TACTICIAN, result);
        assertEquals(InvestorProfile.TACTICIAN, user.getInvestorProfile());
        assertEquals(true, user.hasAnsweredOnboarding());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void execute_WhenUserAlreadyAnsweredOnboarding_ShouldReturnExistingProfileWithoutRecomputing() {
        String email = "investor@test.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(InvestorProfile.GUARDIAN);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        InvestorProfile result = submitAssessmentUseCase.execute(email, ANSWERS);

        assertEquals(InvestorProfile.GUARDIAN, result);
        verify(calculateInvestorProfileUseCase, never()).execute(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        String email = "missing@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                submitAssessmentUseCase.execute(email, ANSWERS));

        verify(userRepository, never()).save(any());
    }
}
