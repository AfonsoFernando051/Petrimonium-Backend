package com.jf.PetApp.infrastructure.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "learning_lessons", schema = "education")
@Getter
@Setter
public class LearningLessonJpaEntity {

    @Id
    @Column(name = "lesson_id")
    private String lessonId;

    @Column(name = "module_id")
    private String moduleId;

    @Column(name = "xp_reward")
    private int xpReward;

    @Column(name = "lesson_order")
    private int lessonOrder;

    /** One of the 8-level competency model (RECOGNIZE .. INTEGRATE) — see DECISION-025. */
    @Column(name = "competency")
    private String competency;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    /** Taxation-only regulatory metadata — null for every other lesson. See DECISION-025. */
    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "last_verified_at")
    private LocalDate lastVerifiedAt;

    @Column(name = "source")
    private String source;
}
