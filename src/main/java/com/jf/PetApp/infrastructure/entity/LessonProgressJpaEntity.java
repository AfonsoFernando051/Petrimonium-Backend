package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lesson_progress")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isPerfectFirstTry() {
        return perfectFirstTry;
    }

    public void setPerfectFirstTry(boolean perfectFirstTry) {
        this.perfectFirstTry = perfectFirstTry;
    }
}
