package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_modules")
public class LearningModuleJpaEntity {

    @Id
    @Column(name = "module_id")
    private String moduleId;

    @Column(name = "xp_reward")
    private int xpReward;

    @Column(name = "module_order")
    private int moduleOrder;

    @Column(name = "lesson_count")
    private int lessonCount;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "icon_key")
    private String iconKey;

    @Column(name = "content_available")
    private boolean contentAvailable;

    /** FOUNDATION .. SPECIALIZATION — see DECISION-025. */
    @Column(name = "difficulty")
    private String difficulty;

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

    public int getModuleOrder() {
        return moduleOrder;
    }

    public void setModuleOrder(int moduleOrder) {
        this.moduleOrder = moduleOrder;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = lessonCount;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public boolean isContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
