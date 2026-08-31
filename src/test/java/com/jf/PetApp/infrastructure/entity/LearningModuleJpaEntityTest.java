package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearningModuleJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        LearningModuleJpaEntity entity = new LearningModuleJpaEntity();

        entity.setModuleId("module_1");
        entity.setXpReward(100);
        entity.setModuleOrder(1);
        entity.setLessonCount(5);
        entity.setSchoolId("school_1");
        entity.setIconKey("module_icon");
        entity.setContentAvailable(true);
        entity.setDifficulty("FOUNDATION");

        assertThat(entity.getModuleId()).isEqualTo("module_1");
        assertThat(entity.getXpReward()).isEqualTo(100);
        assertThat(entity.getModuleOrder()).isEqualTo(1);
        assertThat(entity.getLessonCount()).isEqualTo(5);
        assertThat(entity.getSchoolId()).isEqualTo("school_1");
        assertThat(entity.getIconKey()).isEqualTo("module_icon");
        assertThat(entity.isContentAvailable()).isTrue();
        assertThat(entity.getDifficulty()).isEqualTo("FOUNDATION");
    }
}
