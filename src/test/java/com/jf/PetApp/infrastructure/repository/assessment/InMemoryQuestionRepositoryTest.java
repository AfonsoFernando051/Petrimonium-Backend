package com.jf.PetApp.infrastructure.repository.assessment;

import com.jf.PetApp.core.domain.assessment.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InMemoryQuestionRepositoryTest {

    private final InMemoryQuestionRepository repository = new InMemoryQuestionRepository();

    @Test
    void shouldReturnAllQuestions() {
        List<Question> questions = repository.findAll();

        assertFalse(questions.isEmpty());
        assertEquals(5, questions.size());
        assertEquals("q1", questions.get(0).id());
        assertEquals("q2", questions.get(1).id());
        assertEquals("q3", questions.get(2).id());
        assertEquals("q4", questions.get(3).id());
        assertEquals("q5", questions.get(4).id());
    }
}
