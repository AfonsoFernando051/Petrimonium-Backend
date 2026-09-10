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
@Table(name = "academy_lesson_steps", schema = "education")
@Getter
@Setter
public class AcademyLessonStepJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "lesson_id")
    private String lessonId;

    @Column(name = "step_order")
    private int stepOrder;

    @Column(name = "step_type")
    private String stepType;

    @Column(name = "framing")
    private String framing;

    @Column(name = "correct_option_index")
    private Integer correctOptionIndex;
}
