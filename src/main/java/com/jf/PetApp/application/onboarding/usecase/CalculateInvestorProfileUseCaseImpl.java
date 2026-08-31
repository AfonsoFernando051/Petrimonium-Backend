package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.assessment.Option;
import com.jf.PetApp.core.domain.assessment.Question;
import com.jf.PetApp.core.domain.assessment.UserAssessment;
import com.jf.PetApp.core.port.QuestionRepository;

import java.util.List;

public class CalculateInvestorProfileUseCaseImpl implements CalculateInvestorProfileUseCase {

    private final QuestionRepository questionRepository;

    public CalculateInvestorProfileUseCaseImpl(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public InvestorProfile execute(UserAssessment assessment) {
        List<Question> allQuestions = questionRepository.findAll();

        int totalScore = assessment.selectedOptionIds().stream()
                .mapToInt(optionId -> getOptionScore(allQuestions, optionId))
                .sum();

        if (totalScore <= 4) {
            return InvestorProfile.GUARDIAN;
        } else if (totalScore <= 8) {
            return InvestorProfile.TACTICIAN;
        } else {
            return InvestorProfile.ADVENTURER;
        }
    }

    private int getOptionScore(List<Question> questions, String optionId) {
        return questions.stream()
                .flatMap(q -> q.options().stream())
                .filter(o -> o.id().equals(optionId))
                .map(Option::points)
                .findFirst()
                .orElse(0);
    }
}
