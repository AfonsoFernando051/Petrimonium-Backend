package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.onboarding.usecase.CalculateInvestorProfileUseCase;
import com.jf.PetApp.application.onboarding.usecase.CalculateInvestorProfileUseCaseImpl;
import com.jf.PetApp.core.port.QuestionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnboardingUseCaseConfig {

    @Bean
    public CalculateInvestorProfileUseCase calculateInvestorProfileUseCase(QuestionRepository questionRepository) {
        return new CalculateInvestorProfileUseCaseImpl(questionRepository);
    }
}
