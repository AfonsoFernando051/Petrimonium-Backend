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
@Table(name = "academy_choice_question_option_translations", schema = "education")
@Getter
@Setter
public class AcademyChoiceQuestionOptionTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "lang")
    private String lang;

    @Column(name = "option_text", length = 500)
    private String optionText;
}
