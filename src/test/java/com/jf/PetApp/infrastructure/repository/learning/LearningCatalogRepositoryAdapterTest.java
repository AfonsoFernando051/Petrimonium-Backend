package com.jf.PetApp.infrastructure.repository.learning;

import com.jf.PetApp.application.learning.port.LearningCatalogPort;
import com.jf.PetApp.core.domain.learning.LessonCatalogEntry;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;
import com.jf.PetApp.infrastructure.entity.LearningLessonJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningModuleJpaEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LearningCatalogRepositoryAdapterTest {

    @Autowired
    private LearningLessonJpaRepository lessonRepository;

    @Autowired
    private LearningModuleJpaRepository moduleRepository;

    private LearningCatalogPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new LearningCatalogRepositoryAdapter(lessonRepository, moduleRepository);

        LearningModuleJpaEntity module = new LearningModuleJpaEntity();
        module.setModuleId("module1");
        module.setXpReward(50);
        module.setModuleOrder(1);
        module.setLessonCount(2);
        moduleRepository.save(module);

        LearningLessonJpaEntity lesson1 = new LearningLessonJpaEntity();
        lesson1.setLessonId("lesson1");
        lesson1.setModuleId("module1");
        lesson1.setXpReward(10);
        lesson1.setLessonOrder(1);
        lessonRepository.save(lesson1);

        LearningLessonJpaEntity lesson2 = new LearningLessonJpaEntity();
        lesson2.setLessonId("lesson2");
        lesson2.setModuleId("module1");
        lesson2.setXpReward(10);
        lesson2.setLessonOrder(2);
        lessonRepository.save(lesson2);
    }

    @Test
    void findLesson_WhenExists_MapsAllFieldsToTheDomainRecord() {
        Optional<LessonCatalogEntry> lesson = adapter.findLesson("lesson1");

        assertThat(lesson).isPresent();
        assertThat(lesson.get().lessonId()).isEqualTo("lesson1");
        assertThat(lesson.get().moduleId()).isEqualTo("module1");
        assertThat(lesson.get().xpReward()).isEqualTo(10);
        assertThat(lesson.get().order()).isEqualTo(1);
    }

    @Test
    void findLesson_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findLesson("unknown_lesson")).isEmpty();
    }

    @Test
    void findModule_WhenExists_MapsAllFieldsToTheDomainRecord() {
        Optional<ModuleCatalogEntry> module = adapter.findModule("module1");

        assertThat(module).isPresent();
        assertThat(module.get().moduleId()).isEqualTo("module1");
        assertThat(module.get().xpReward()).isEqualTo(50);
        assertThat(module.get().order()).isEqualTo(1);
        assertThat(module.get().lessonCount()).isEqualTo(2);
    }

    @Test
    void findModule_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findModule("unknown_module")).isEmpty();
    }

    @Test
    void lessonIdsForModule_ReturnsOnlyLessonsBelongingToThatModule() {
        List<String> lessonIds = adapter.lessonIdsForModule("module1");

        assertThat(lessonIds).containsExactlyInAnyOrder("lesson1", "lesson2");
    }

    @Test
    void allModules_ReturnsEveryModuleMappedToTheDomainRecord() {
        List<ModuleCatalogEntry> modules = adapter.allModules();

        assertThat(modules).hasSize(1);
        assertThat(modules.get(0).moduleId()).isEqualTo("module1");
    }
}
