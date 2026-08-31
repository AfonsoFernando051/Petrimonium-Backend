package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LearningLessonJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        LearningLessonJpaEntity entity = new LearningLessonJpaEntity();
        LocalDate effectiveDate = LocalDate.of(2024, 1, 1);
        LocalDate lastVerifiedAt = LocalDate.of(2024, 6, 1);

        entity.setLessonId("lesson_1");
        entity.setModuleId("module_1");
        entity.setXpReward(50);
        entity.setLessonOrder(1);
        entity.setCompetency("APPLY");
        entity.setEstimatedMinutes(10);
        entity.setJurisdiction("PT");
        entity.setEffectiveDate(effectiveDate);
        entity.setLastVerifiedAt(lastVerifiedAt);
        entity.setSource("tax_authority");

        assertThat(entity.getLessonId()).isEqualTo("lesson_1");
        assertThat(entity.getModuleId()).isEqualTo("module_1");
        assertThat(entity.getXpReward()).isEqualTo(50);
        assertThat(entity.getLessonOrder()).isEqualTo(1);
        assertThat(entity.getCompetency()).isEqualTo("APPLY");
        assertThat(entity.getEstimatedMinutes()).isEqualTo(10);
        assertThat(entity.getJurisdiction()).isEqualTo("PT");
        assertThat(entity.getEffectiveDate()).isEqualTo(effectiveDate);
        assertThat(entity.getLastVerifiedAt()).isEqualTo(lastVerifiedAt);
        assertThat(entity.getSource()).isEqualTo("tax_authority");
    }
}
