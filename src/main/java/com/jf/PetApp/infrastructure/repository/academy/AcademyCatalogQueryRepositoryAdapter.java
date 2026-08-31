package com.jf.PetApp.infrastructure.repository.academy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonStepView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;
import com.jf.PetApp.application.academy.port.AcademyCatalogQueryPort;
import com.jf.PetApp.infrastructure.config.CacheConfig;
import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyDomainJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyDomainTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonPortfolioConceptJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTakeawayJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTakeawayTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyModulePrerequisiteJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyModuleTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolPrerequisiteJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolTranslationJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningLessonJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningModuleJpaEntity;
import com.jf.PetApp.infrastructure.repository.learning.LearningLessonJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LearningModuleJpaRepository;

/**
 * The only place that knows the Academy catalog is stored as JPA entities
 * split into structural tables + one translation row per (entity, lang) —
 * see {@code V10__academy_content_schema.sql}. Every join here is done in
 * memory (fetch-all + group-by), not via JPQL joins: the catalog is small
 * (tens of schools, low hundreds of lessons/steps today), and this mirrors
 * {@code LearningCatalogRepositoryAdapter}'s existing "manual mapping, no
 * query magic" convention.
 */
@Repository
public class AcademyCatalogQueryRepositoryAdapter implements AcademyCatalogQueryPort {

    private final AcademyDomainJpaRepository domainRepository;
    private final AcademyDomainTranslationJpaRepository domainTranslationRepository;
    private final AcademySchoolJpaRepository schoolRepository;
    private final AcademySchoolTranslationJpaRepository schoolTranslationRepository;
    private final AcademySchoolPrerequisiteJpaRepository schoolPrerequisiteRepository;
    private final LearningModuleJpaRepository moduleRepository;
    private final AcademyModuleTranslationJpaRepository moduleTranslationRepository;
    private final AcademyModulePrerequisiteJpaRepository modulePrerequisiteRepository;
    private final LearningLessonJpaRepository lessonRepository;
    private final AcademyLessonTranslationJpaRepository lessonTranslationRepository;
    private final AcademyLessonPortfolioConceptJpaRepository lessonPortfolioConceptRepository;
    private final AcademyLessonStepJpaRepository stepRepository;
    private final AcademyLessonStepTranslationJpaRepository stepTranslationRepository;
    private final AcademyChoiceQuestionOptionJpaRepository optionRepository;
    private final AcademyChoiceQuestionOptionTranslationJpaRepository optionTranslationRepository;
    private final AcademyLessonStepTakeawayJpaRepository takeawayRepository;
    private final AcademyLessonStepTakeawayTranslationJpaRepository takeawayTranslationRepository;

    public AcademyCatalogQueryRepositoryAdapter(
            AcademyDomainJpaRepository domainRepository,
            AcademyDomainTranslationJpaRepository domainTranslationRepository,
            AcademySchoolJpaRepository schoolRepository,
            AcademySchoolTranslationJpaRepository schoolTranslationRepository,
            AcademySchoolPrerequisiteJpaRepository schoolPrerequisiteRepository,
            LearningModuleJpaRepository moduleRepository,
            AcademyModuleTranslationJpaRepository moduleTranslationRepository,
            AcademyModulePrerequisiteJpaRepository modulePrerequisiteRepository,
            LearningLessonJpaRepository lessonRepository,
            AcademyLessonTranslationJpaRepository lessonTranslationRepository,
            AcademyLessonPortfolioConceptJpaRepository lessonPortfolioConceptRepository,
            AcademyLessonStepJpaRepository stepRepository,
            AcademyLessonStepTranslationJpaRepository stepTranslationRepository,
            AcademyChoiceQuestionOptionJpaRepository optionRepository,
            AcademyChoiceQuestionOptionTranslationJpaRepository optionTranslationRepository,
            AcademyLessonStepTakeawayJpaRepository takeawayRepository,
            AcademyLessonStepTakeawayTranslationJpaRepository takeawayTranslationRepository) {
        this.domainRepository = domainRepository;
        this.domainTranslationRepository = domainTranslationRepository;
        this.schoolRepository = schoolRepository;
        this.schoolTranslationRepository = schoolTranslationRepository;
        this.schoolPrerequisiteRepository = schoolPrerequisiteRepository;
        this.moduleRepository = moduleRepository;
        this.moduleTranslationRepository = moduleTranslationRepository;
        this.modulePrerequisiteRepository = modulePrerequisiteRepository;
        this.lessonRepository = lessonRepository;
        this.lessonTranslationRepository = lessonTranslationRepository;
        this.lessonPortfolioConceptRepository = lessonPortfolioConceptRepository;
        this.stepRepository = stepRepository;
        this.stepTranslationRepository = stepTranslationRepository;
        this.optionRepository = optionRepository;
        this.optionTranslationRepository = optionTranslationRepository;
        this.takeawayRepository = takeawayRepository;
        this.takeawayTranslationRepository = takeawayTranslationRepository;
    }

    @Override
    @Cacheable(CacheConfig.ACADEMY_CATALOG_CACHE)
    public AcademyCatalogResult getCatalog(String lang) {
        List<AcademySchoolJpaEntity> schools = schoolRepository.findAll().stream()
                .sorted(Comparator.comparingInt(AcademySchoolJpaEntity::getOrderIndex))
                .toList();
        // learning_modules predates academy_schools (see V4/V9 vs V10) and ids are
        // never deleted (AcademyContentSeedRunner's class doc), so a module id that
        // has since dropped out of the current JSON content — never revisited by
        // the seeder's upsert-by-id — can still sit in the table with a null
        // school_id from before that column existed. Such a row isn't part of the
        // current curriculum tree and the client's schema has no way to represent
        // it (schoolId is non-nullable there), so it's excluded here rather than
        // shipped as broken JSON.
        List<LearningModuleJpaEntity> modules = moduleRepository.findAll().stream()
                .filter(m -> m.getSchoolId() != null)
                .sorted(Comparator.comparingInt(LearningModuleJpaEntity::getModuleOrder))
                .toList();
        Set<String> moduleIds = modules.stream().map(LearningModuleJpaEntity::getModuleId).collect(Collectors.toSet());
        List<LearningLessonJpaEntity> lessons = lessonRepository.findAll().stream()
                .filter(l -> moduleIds.contains(l.getModuleId()))
                .sorted(Comparator.comparingInt(LearningLessonJpaEntity::getLessonOrder))
                .toList();
        List<AcademyLessonStepJpaEntity> steps = stepRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademyLessonStepJpaEntity::getLessonId)
                        .thenComparingInt(AcademyLessonStepJpaEntity::getStepOrder))
                .toList();

        return new AcademyCatalogResult(
                buildDomains(lang, schools),
                buildSchools(lang, schools),
                buildModules(lang, modules, lessons),
                buildLessons(lang, lessons, steps));
    }

    private List<AcademyDomainView> buildDomains(String lang, List<AcademySchoolJpaEntity> schools) {
        Map<String, AcademyDomainTranslationJpaEntity> translationByDomainId =
                indexBy(domainTranslationRepository.findByLang(lang), AcademyDomainTranslationJpaEntity::getDomainId);
        Map<String, List<String>> schoolIdsByDomainId = schools.stream()
                .collect(Collectors.groupingBy(
                        AcademySchoolJpaEntity::getDomainId,
                        Collectors.mapping(AcademySchoolJpaEntity::getSchoolId, Collectors.toList())));

        return domainRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(d -> {
                    AcademyDomainTranslationJpaEntity t = translationByDomainId.get(d.getDomainId());
                    return new AcademyDomainView(
                            d.getDomainId(),
                            t != null ? t.getTitle() : "",
                            t != null ? t.getDescription() : "",
                            d.getIconKey(),
                            d.getOrderIndex(),
                            schoolIdsByDomainId.getOrDefault(d.getDomainId(), List.of()));
                })
                .toList();
    }

    private List<AcademySchoolView> buildSchools(String lang, List<AcademySchoolJpaEntity> schools) {
        Map<String, AcademySchoolTranslationJpaEntity> translationBySchoolId =
                indexBy(schoolTranslationRepository.findByLang(lang), AcademySchoolTranslationJpaEntity::getSchoolId);
        Map<String, List<String>> prerequisitesBySchoolId = schoolPrerequisiteRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        AcademySchoolPrerequisiteJpaEntity::getSchoolId,
                        Collectors.mapping(AcademySchoolPrerequisiteJpaEntity::getPrerequisiteSchoolId, Collectors.toList())));

        return schools.stream()
                .map(s -> {
                    AcademySchoolTranslationJpaEntity t = translationBySchoolId.get(s.getSchoolId());
                    return new AcademySchoolView(
                            s.getSchoolId(),
                            s.getDomainId(),
                            t != null ? t.getTitle() : "",
                            t != null ? t.getDescription() : "",
                            s.getIconKey(),
                            s.getOrderIndex(),
                            prerequisitesBySchoolId.getOrDefault(s.getSchoolId(), List.of()),
                            s.isContentAvailable());
                })
                .toList();
    }

    private List<AcademyModuleView> buildModules(
            String lang, List<LearningModuleJpaEntity> modules, List<LearningLessonJpaEntity> lessons) {
        Map<String, AcademyModuleTranslationJpaEntity> translationByModuleId =
                indexBy(moduleTranslationRepository.findByLang(lang), AcademyModuleTranslationJpaEntity::getModuleId);
        Map<String, List<String>> prerequisitesByModuleId = modulePrerequisiteRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        AcademyModulePrerequisiteJpaEntity::getModuleId,
                        Collectors.mapping(AcademyModulePrerequisiteJpaEntity::getPrerequisiteModuleId, Collectors.toList())));
        // lessons is already sorted by lessonOrder (see getCatalog), so grouping preserves order.
        Map<String, List<String>> lessonIdsByModuleId = lessons.stream()
                .collect(Collectors.groupingBy(
                        LearningLessonJpaEntity::getModuleId,
                        Collectors.mapping(LearningLessonJpaEntity::getLessonId, Collectors.toList())));

        return modules.stream()
                .map(m -> {
                    AcademyModuleTranslationJpaEntity t = translationByModuleId.get(m.getModuleId());
                    return new AcademyModuleView(
                            m.getModuleId(),
                            m.getSchoolId(),
                            t != null ? t.getTitle() : "",
                            t != null ? t.getDescription() : "",
                            m.getIconKey(),
                            m.getDifficulty(),
                            m.getModuleOrder(),
                            lessonIdsByModuleId.getOrDefault(m.getModuleId(), List.of()),
                            prerequisitesByModuleId.getOrDefault(m.getModuleId(), List.of()),
                            m.isContentAvailable());
                })
                .toList();
    }

    private List<AcademyLessonView> buildLessons(
            String lang, List<LearningLessonJpaEntity> lessons, List<AcademyLessonStepJpaEntity> steps) {
        Map<String, AcademyLessonTranslationJpaEntity> translationByLessonId =
                indexBy(lessonTranslationRepository.findByLang(lang), AcademyLessonTranslationJpaEntity::getLessonId);
        Map<String, List<String>> portfolioConceptsByLessonId = lessonPortfolioConceptRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        AcademyLessonPortfolioConceptJpaEntity::getLessonId,
                        Collectors.mapping(AcademyLessonPortfolioConceptJpaEntity::getConceptId, Collectors.toList())));
        Map<String, List<AcademyLessonStepJpaEntity>> stepsByLessonId =
                steps.stream().collect(Collectors.groupingBy(AcademyLessonStepJpaEntity::getLessonId));

        Map<Long, AcademyLessonStepTranslationJpaEntity> stepTranslationByStepId =
                indexBy(stepTranslationRepository.findByLang(lang), AcademyLessonStepTranslationJpaEntity::getStepId);

        List<AcademyChoiceQuestionOptionJpaEntity> allOptions = optionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(AcademyChoiceQuestionOptionJpaEntity::getPosition))
                .toList();
        Map<Long, List<AcademyChoiceQuestionOptionJpaEntity>> optionsByStepId =
                allOptions.stream().collect(Collectors.groupingBy(AcademyChoiceQuestionOptionJpaEntity::getStepId));
        Map<Long, AcademyChoiceQuestionOptionTranslationJpaEntity> optionTranslationByOptionId = indexBy(
                optionTranslationRepository.findByLang(lang), AcademyChoiceQuestionOptionTranslationJpaEntity::getOptionId);

        List<AcademyLessonStepTakeawayJpaEntity> allTakeaways = takeawayRepository.findAll().stream()
                .sorted(Comparator.comparingInt(AcademyLessonStepTakeawayJpaEntity::getPosition))
                .toList();
        Map<Long, List<AcademyLessonStepTakeawayJpaEntity>> takeawaysByStepId =
                allTakeaways.stream().collect(Collectors.groupingBy(AcademyLessonStepTakeawayJpaEntity::getStepId));
        Map<Long, AcademyLessonStepTakeawayTranslationJpaEntity> takeawayTranslationByTakeawayId = indexBy(
                takeawayTranslationRepository.findByLang(lang), AcademyLessonStepTakeawayTranslationJpaEntity::getTakeawayId);

        return lessons.stream()
                .map(l -> {
                    AcademyLessonTranslationJpaEntity t = translationByLessonId.get(l.getLessonId());
                    List<AcademyLessonStepView> stepViews = stepsByLessonId.getOrDefault(l.getLessonId(), List.of()).stream()
                            .map(step -> buildStep(
                                    step, stepTranslationByStepId, optionsByStepId, optionTranslationByOptionId,
                                    takeawaysByStepId, takeawayTranslationByTakeawayId))
                            .toList();
                    return new AcademyLessonView(
                            l.getLessonId(), l.getModuleId(), t != null ? t.getTitle() : "",
                            t != null ? t.getLearningObjective() : null, l.getCompetency(), l.getEstimatedMinutes(),
                            l.getLessonOrder(), l.getXpReward(),
                            portfolioConceptsByLessonId.getOrDefault(l.getLessonId(), List.of()), stepViews);
                })
                .toList();
    }

    private AcademyLessonStepView buildStep(
            AcademyLessonStepJpaEntity step,
            Map<Long, AcademyLessonStepTranslationJpaEntity> stepTranslationByStepId,
            Map<Long, List<AcademyChoiceQuestionOptionJpaEntity>> optionsByStepId,
            Map<Long, AcademyChoiceQuestionOptionTranslationJpaEntity> optionTranslationByOptionId,
            Map<Long, List<AcademyLessonStepTakeawayJpaEntity>> takeawaysByStepId,
            Map<Long, AcademyLessonStepTakeawayTranslationJpaEntity> takeawayTranslationByTakeawayId) {
        AcademyLessonStepTranslationJpaEntity t = stepTranslationByStepId.get(step.getId());

        List<String> options = optionsByStepId.getOrDefault(step.getId(), List.of()).stream()
                .map(o -> {
                    AcademyChoiceQuestionOptionTranslationJpaEntity ot = optionTranslationByOptionId.get(o.getId());
                    return ot != null ? ot.getOptionText() : "";
                })
                .toList();

        List<String> takeaways = takeawaysByStepId.getOrDefault(step.getId(), List.of()).stream()
                .map(tk -> {
                    AcademyLessonStepTakeawayTranslationJpaEntity tt = takeawayTranslationByTakeawayId.get(tk.getId());
                    return tt != null ? tt.getTakeawayText() : "";
                })
                .toList();

        return new AcademyLessonStepView(
                step.getStepType().toLowerCase(Locale.ROOT),
                t != null ? t.getTitle() : null,
                t != null ? t.getBody() : null,
                step.getFraming() != null ? step.getFraming().toLowerCase(Locale.ROOT) : null,
                options,
                step.getCorrectOptionIndex(),
                t != null ? t.getPrompt() : null,
                t != null ? t.getExplanation() : null,
                takeaways);
    }

    private static <K, V> Map<K, V> indexBy(List<V> values, Function<V, K> keyFn) {
        return values.stream().collect(Collectors.toMap(keyFn, Function.identity(), (a, b) -> a));
    }
}
