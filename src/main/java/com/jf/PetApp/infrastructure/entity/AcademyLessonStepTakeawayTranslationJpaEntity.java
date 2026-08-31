package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academy_lesson_step_takeaway_translations")
public class AcademyLessonStepTakeawayTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "takeaway_id")
    private Long takeawayId;

    @Column(name = "lang")
    private String lang;

    @Column(name = "takeaway_text", length = 500)
    private String takeawayText;

    public Long getId() {
        return id;
    }

    public Long getTakeawayId() {
        return takeawayId;
    }

    public void setTakeawayId(Long takeawayId) {
        this.takeawayId = takeawayId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getTakeawayText() {
        return takeawayText;
    }

    public void setTakeawayText(String takeawayText) {
        this.takeawayText = takeawayText;
    }
}
