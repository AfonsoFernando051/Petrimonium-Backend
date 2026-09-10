package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lesson_progress", schema = "education")
@Getter
@Setter
public class LessonProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "lesson_id")
    private String lessonId;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Every question in this lesson answered correctly on the first try, at least once. Monotonic — see DECISION-025. */
    @Column(name = "perfect_first_try")
    private boolean perfectFirstTry;
}
