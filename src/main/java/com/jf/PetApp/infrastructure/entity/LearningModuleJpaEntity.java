package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "learning_modules", schema = "education")
@Getter
@Setter
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
}
