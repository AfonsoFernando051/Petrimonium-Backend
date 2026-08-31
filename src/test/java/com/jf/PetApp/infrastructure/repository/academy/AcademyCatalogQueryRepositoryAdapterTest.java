package com.jf.PetApp.infrastructure.repository.academy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyDomainJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyDomainTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonPortfolioConceptJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyModuleTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningLessonJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningModuleJpaEntity;
import com.jf.PetApp.infrastructure.repository.learning.LearningLessonJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LearningModuleJpaRepository;

@DataJpaTest
class AcademyCatalogQueryRepositoryAdapterTest {

    @Autowired
    private AcademyDomainJpaRepository domainRepository;
    @Autowired
    private AcademyDomainTranslationJpaRepository domainTranslationRepository;
    @Autowired
    private AcademySchoolJpaRepository schoolRepository;
    @Autowired
    private AcademySchoolTranslationJpaRepository schoolTranslationRepository;
    @Autowired
    private AcademySchoolPrerequisiteJpaRepository schoolPrerequisiteRepository;
    @Autowired
    private LearningModuleJpaRepository moduleRepository;
    @Autowired
    private AcademyModuleTranslationJpaRepository moduleTranslationRepository;
    @Autowired
    private AcademyModulePrerequisiteJpaRepository modulePrerequisiteRepository;
    @Autowired
    private LearningLessonJpaRepository lessonRepository;
    @Autowired
    private AcademyLessonTranslationJpaRepository lessonTranslationRepository;
    @Autowired
    private AcademyLessonPortfolioConceptJpaRepository lessonPortfolioConceptRepository;
    @Autowired
    private AcademyLessonStepJpaRepository stepRepository;
    @Autowired
    private AcademyLessonStepTranslationJpaRepository stepTranslationRepository;
    @Autowired
    private AcademyChoiceQuestionOptionJpaRepository optionRepository;
    @Autowired
    private AcademyChoiceQuestionOptionTranslationJpaRepository optionTranslationRepository;
    @Autowired
    private AcademyLessonStepTakeawayJpaRepository takeawayRepository;
    @Autowired
    private AcademyLessonStepTakeawayTranslationJpaRepository takeawayTranslationRepository;

    private AcademyCatalogQueryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AcademyCatalogQueryRepositoryAdapter(
                domainRepository, domainTranslationRepository,
                schoolRepository, schoolTranslationRepository, schoolPrerequisiteRepository,
                moduleRepository, moduleTranslationRepository, modulePrerequisiteRepository,
                lessonRepository, lessonTranslationRepository, lessonPortfolioConceptRepository,
                stepRepository, stepTranslationRepository,
                optionRepository, optionTranslationRepository,
                takeawayRepository, takeawayTranslationRepository);

        AcademyDomainJpaEntity domain = new AcademyDomainJpaEntity();
        domain.setDomainId("financial_education");
        domain.setOrderIndex(1);
        domain.setIconKey("savings_outlined");
        domainRepository.save(domain);
        saveDomainTranslation("financial_education", "pt", "Educação Financeira", "Descrição PT");
        saveDomainTranslation("financial_education", "en", "Financial Education", "Description EN");

        AcademySchoolJpaEntity school = new AcademySchoolJpaEntity();
        school.setSchoolId("financial_life");
        school.setDomainId("financial_education");
        school.setOrderIndex(1);
        school.setIconKey("savings_outlined");
        school.setContentAvailable(true);
        schoolRepository.save(school);
        saveSchoolTranslation("financial_life", "pt", "Vida Financeira", "Descrição PT");
        saveSchoolTranslation("financial_life", "en", "Financial Life", "Description EN");

        LearningModuleJpaEntity module = new LearningModuleJpaEntity();
        module.setModuleId("money_fundamentals");
        module.setXpReward(100);
        module.setModuleOrder(1);
        module.setLessonCount(1);
        module.setSchoolId("financial_life");
        module.setIconKey("payments_outlined");
        module.setContentAvailable(true);
        moduleRepository.save(module);
        saveModuleTranslation("money_fundamentals", "pt", "Fundamentos do Dinheiro", "Descrição PT");
        saveModuleTranslation("money_fundamentals", "en", "Money Fundamentals", "Description EN");

        LearningLessonJpaEntity lesson = new LearningLessonJpaEntity();
        lesson.setLessonId("money_fundamentals_what_is_money");
        lesson.setModuleId("money_fundamentals");
        lesson.setXpReward(20);
        lesson.setLessonOrder(1);
        lessonRepository.save(lesson);
        saveLessonTranslation("money_fundamentals_what_is_money", "pt", "O que é Dinheiro?");
        saveLessonTranslation("money_fundamentals_what_is_money", "en", "What Is Money?");
        savePortfolioConcept("money_fundamentals_what_is_money", "pe");
        savePortfolioConcept("money_fundamentals_what_is_money", "dy");

        AcademyLessonStepJpaEntity step = new AcademyLessonStepJpaEntity();
        step.setLessonId("money_fundamentals_what_is_money");
        step.setStepOrder(1);
        step.setStepType("CHOICE_QUESTION");
        step.setFraming("MICRO_EXERCISE");
        step.setCorrectOptionIndex(1);
        step = stepRepository.save(step);

        saveStepTranslation(step.getId(), "pt", null, null, "Por que?", "Porque sim.");
        saveStepTranslation(step.getId(), "en", null, null, "Why?", "Because.");

        AcademyChoiceQuestionOptionJpaEntity option0 = new AcademyChoiceQuestionOptionJpaEntity();
        option0.setStepId(step.getId());
        option0.setPosition(0);
        option0 = optionRepository.save(option0);
        saveOptionTranslation(option0.getId(), "pt", "Opção A");
        saveOptionTranslation(option0.getId(), "en", "Option A");

        AcademyChoiceQuestionOptionJpaEntity option1 = new AcademyChoiceQuestionOptionJpaEntity();
        option1.setStepId(step.getId());
        option1.setPosition(1);
        option1 = optionRepository.save(option1);
        saveOptionTranslation(option1.getId(), "pt", "Opção B");
        saveOptionTranslation(option1.getId(), "en", "Option B");
    }

    @Test
    void getCatalog_ResolvesEveryLevelToTheRequestedLanguage() {
        AcademyCatalogResult pt = adapter.getCatalog("pt");

        assertThat(pt.domains()).hasSize(1);
        assertThat(pt.domains().get(0).title()).isEqualTo("Educação Financeira");
        assertThat(pt.domains().get(0).schoolIds()).containsExactly("financial_life");

        assertThat(pt.schools()).hasSize(1);
        assertThat(pt.schools().get(0).title()).isEqualTo("Vida Financeira");
        assertThat(pt.schools().get(0).domainId()).isEqualTo("financial_education");

        assertThat(pt.modules()).hasSize(1);
        assertThat(pt.modules().get(0).title()).isEqualTo("Fundamentos do Dinheiro");
        assertThat(pt.modules().get(0).lessonIds()).containsExactly("money_fundamentals_what_is_money");

        assertThat(pt.lessons()).hasSize(1);
        assertThat(pt.lessons().get(0).title()).isEqualTo("O que é Dinheiro?");
        assertThat(pt.lessons().get(0).portfolioConcepts()).containsExactlyInAnyOrder("pe", "dy");
        assertThat(pt.lessons().get(0).steps()).hasSize(1);
        assertThat(pt.lessons().get(0).steps().get(0).prompt()).isEqualTo("Por que?");
        assertThat(pt.lessons().get(0).steps().get(0).options()).containsExactly("Opção A", "Opção B");
        assertThat(pt.lessons().get(0).steps().get(0).correctIndex()).isEqualTo(1);
        assertThat(pt.lessons().get(0).steps().get(0).type()).isEqualTo("choice_question");
        assertThat(pt.lessons().get(0).steps().get(0).framing()).isEqualTo("micro_exercise");
    }

    @Test
    void getCatalog_ExcludesOrphanedModulesWithNoSchoolId() {
        // A module row with a null school_id predates academy_schools (see
        // V4/V9 vs V10) and can linger for an id no current content JSON
        // references — AcademyContentSeedRunner never deletes old ids. Such a
        // row must not reach the client: AcademyModuleView.schoolId is
        // non-nullable there and the Flutter model does an unchecked `as
        // String` cast that crashes on null.
        LearningModuleJpaEntity orphanModule = new LearningModuleJpaEntity();
        orphanModule.setModuleId("legacy_orphan_module");
        orphanModule.setXpReward(50);
        orphanModule.setModuleOrder(99);
        orphanModule.setLessonCount(1);
        orphanModule.setSchoolId(null);
        moduleRepository.save(orphanModule);

        LearningLessonJpaEntity orphanLesson = new LearningLessonJpaEntity();
        orphanLesson.setLessonId("legacy_orphan_lesson");
        orphanLesson.setModuleId("legacy_orphan_module");
        orphanLesson.setXpReward(10);
        orphanLesson.setLessonOrder(1);
        lessonRepository.save(orphanLesson);

        AcademyCatalogResult pt = adapter.getCatalog("pt");

        assertThat(pt.modules()).extracting("id").doesNotContain("legacy_orphan_module");
        assertThat(pt.lessons()).extracting("id").doesNotContain("legacy_orphan_lesson");
        assertThat(pt.modules()).hasSize(1);
        assertThat(pt.lessons()).hasSize(1);
    }

    @Test
    void getCatalog_WithDifferentLang_ReturnsDifferentText() {
        AcademyCatalogResult en = adapter.getCatalog("en");

        assertThat(en.domains().get(0).title()).isEqualTo("Financial Education");
        assertThat(en.schools().get(0).title()).isEqualTo("Financial Life");
        assertThat(en.modules().get(0).title()).isEqualTo("Money Fundamentals");
        assertThat(en.lessons().get(0).title()).isEqualTo("What Is Money?");
        assertThat(en.lessons().get(0).steps().get(0).prompt()).isEqualTo("Why?");
        assertThat(en.lessons().get(0).steps().get(0).options()).containsExactly("Option A", "Option B");
    }

    private void saveDomainTranslation(String domainId, String lang, String title, String description) {
        AcademyDomainTranslationJpaEntity t = new AcademyDomainTranslationJpaEntity();
        t.setDomainId(domainId);
        t.setLang(lang);
        t.setTitle(title);
        t.setDescription(description);
        domainTranslationRepository.save(t);
    }

    private void saveSchoolTranslation(String schoolId, String lang, String title, String description) {
        AcademySchoolTranslationJpaEntity t = new AcademySchoolTranslationJpaEntity();
        t.setSchoolId(schoolId);
        t.setLang(lang);
        t.setTitle(title);
        t.setDescription(description);
        schoolTranslationRepository.save(t);
    }

    private void saveModuleTranslation(String moduleId, String lang, String title, String description) {
        AcademyModuleTranslationJpaEntity t = new AcademyModuleTranslationJpaEntity();
        t.setModuleId(moduleId);
        t.setLang(lang);
        t.setTitle(title);
        t.setDescription(description);
        moduleTranslationRepository.save(t);
    }

    private void saveLessonTranslation(String lessonId, String lang, String title) {
        AcademyLessonTranslationJpaEntity t = new AcademyLessonTranslationJpaEntity();
        t.setLessonId(lessonId);
        t.setLang(lang);
        t.setTitle(title);
        lessonTranslationRepository.save(t);
    }

    private void savePortfolioConcept(String lessonId, String conceptId) {
        AcademyLessonPortfolioConceptJpaEntity e = new AcademyLessonPortfolioConceptJpaEntity();
        e.setLessonId(lessonId);
        e.setConceptId(conceptId);
        lessonPortfolioConceptRepository.save(e);
    }

    private void saveStepTranslation(Long stepId, String lang, String title, String body, String prompt, String explanation) {
        AcademyLessonStepTranslationJpaEntity t = new AcademyLessonStepTranslationJpaEntity();
        t.setStepId(stepId);
        t.setLang(lang);
        t.setTitle(title);
        t.setBody(body);
        t.setPrompt(prompt);
        t.setExplanation(explanation);
        stepTranslationRepository.save(t);
    }

    private void saveOptionTranslation(Long optionId, String lang, String optionText) {
        AcademyChoiceQuestionOptionTranslationJpaEntity t = new AcademyChoiceQuestionOptionTranslationJpaEntity();
        t.setOptionId(optionId);
        t.setLang(lang);
        t.setOptionText(optionText);
        optionTranslationRepository.save(t);
    }
}
