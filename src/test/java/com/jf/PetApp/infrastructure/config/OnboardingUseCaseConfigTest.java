package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.onboarding.usecase.CalculateInvestorProfileUseCase;
import com.jf.PetApp.application.onboarding.usecase.CalculateInvestorProfileUseCaseImpl;
import com.jf.PetApp.core.port.QuestionRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class OnboardingUseCaseConfigTest {

    private final OnboardingUseCaseConfig config = new OnboardingUseCaseConfig();

    @Test
    void calculateInvestorProfileUseCase_WiresUpACalculateInvestorProfileUseCaseImpl() {
        CalculateInvestorProfileUseCase useCase =
                config.calculateInvestorProfileUseCase(mock(QuestionRepository.class));

        assertInstanceOf(CalculateInvestorProfileUseCaseImpl.class, useCase);
    }
}
