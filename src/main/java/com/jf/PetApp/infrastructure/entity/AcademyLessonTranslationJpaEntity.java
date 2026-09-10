package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academy_lesson_translations", schema = "education")
@Getter
@Setter
public class AcademyLessonTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "lesson_id")
    private String lessonId;

    @Column(name = "lang")
    private String lang;

    @Column(name = "title")
    private String title;

    /** What the learner can DO after this lesson — see DECISION-025. Nullable: not yet authored for every lesson. */
    @Column(name = "learning_objective")
    private String learningObjective;
}
