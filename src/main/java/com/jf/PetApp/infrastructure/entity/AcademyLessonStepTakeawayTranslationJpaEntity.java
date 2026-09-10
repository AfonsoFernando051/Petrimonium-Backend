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
@Table(name = "academy_lesson_step_takeaway_translations", schema = "education")
@Getter
@Setter
public class AcademyLessonStepTakeawayTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "takeaway_id")
    private Long takeawayId;

    @Column(name = "lang")
    private String lang;

    @Column(name = "takeaway_text", length = 500)
    private String takeawayText;
}
