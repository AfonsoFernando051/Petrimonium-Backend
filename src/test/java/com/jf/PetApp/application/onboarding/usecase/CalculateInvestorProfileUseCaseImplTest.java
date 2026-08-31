package com.jf.PetApp.application.onboarding.usecase;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.assessment.Option;
import com.jf.PetApp.core.domain.assessment.Question;
import com.jf.PetApp.core.domain.assessment.UserAssessment;
import com.jf.PetApp.core.port.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculateInvestorProfileUseCaseImplTest {

    private CalculateInvestorProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        QuestionRepository mockRepo = () -> List.of(
            new Question("q1", "Q1", List.of(
                new Option("o1_0", "A", 0),
                new Option("o1_2", "B", 2),
                new Option("o1_4", "C", 4)
            )),
            new Question("q2", "Q2", List.of(
                new Option("o2_0", "A", 0),
                new Option("o2_2", "B", 2),
                new Option("o2_4", "C", 4)
            ))
        );
        useCase = new CalculateInvestorProfileUseCaseImpl(mockRepo);
    }

    @Test
    void shouldReturnGuardianForScore0to4() {
        // Score = 0 (o1_0, o2_0)
        UserAssessment assessment1 = new UserAssessment(1L, List.of("o1_0", "o2_0"));
        assertEquals(InvestorProfile.GUARDIAN, useCase.execute(assessment1));

        // Score = 4 (o1_4, o2_0)
        UserAssessment assessment2 = new UserAssessment(1L, List.of("o1_4", "o2_0"));
        assertEquals(InvestorProfile.GUARDIAN, useCase.execute(assessment2));
    }

    @Test
    void shouldReturnTacticianForScore5to8() {
        // Score = 6 (o1_2, o2_4)
        UserAssessment assessment = new UserAssessment(1L, List.of("o1_2", "o2_4"));
        assertEquals(InvestorProfile.TACTICIAN, useCase.execute(assessment));

        // Score = 8 (o1_4, o2_4)
        UserAssessment assessment2 = new UserAssessment(1L, List.of("o1_4", "o2_4"));
        assertEquals(InvestorProfile.TACTICIAN, useCase.execute(assessment2));
    }

    @Test
    void shouldReturnAdventurerForScoreGreaterThan8() {
        // Let's mock a 3rd question just for this test to get > 8
        QuestionRepository mockRepoAdv = () -> List.of(
            new Question("q1", "Q1", List.of(new Option("o1_4", "C", 4))),
            new Question("q2", "Q2", List.of(new Option("o2_4", "C", 4))),
            new Question("q3", "Q3", List.of(new Option("o3_4", "C", 4)))
        );
        CalculateInvestorProfileUseCase useCaseAdv = new CalculateInvestorProfileUseCaseImpl(mockRepoAdv);

        // Score = 12
        UserAssessment assessment = new UserAssessment(1L, List.of("o1_4", "o2_4", "o3_4"));
        assertEquals(InvestorProfile.ADVENTURER, useCaseAdv.execute(assessment));
    }

    @Test
    void shouldIgnoreUnknownOptionIdsAndScoreZero() {
        // Unknown IDs score 0
        UserAssessment assessment = new UserAssessment(1L, List.of("invalid_id", "another_invalid"));
        assertEquals(InvestorProfile.GUARDIAN, useCase.execute(assessment));
    }
}
