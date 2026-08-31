package com.jf.PetApp.infrastructure.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_lessons", schema = "education")
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

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public int getLessonOrder() {
        return lessonOrder;
    }

    public void setLessonOrder(int lessonOrder) {
        this.lessonOrder = lessonOrder;
    }

    public String getCompetency() {
        return competency;
    }

    public void setCompetency(String competency) {
        this.competency = competency;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(LocalDate lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
