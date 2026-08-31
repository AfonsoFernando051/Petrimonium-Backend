package com.jf.PetApp.core.port;

import com.jf.PetApp.core.domain.assessment.Question;

import java.util.List;

public interface QuestionRepository {
    List<Question> findAll();
}
