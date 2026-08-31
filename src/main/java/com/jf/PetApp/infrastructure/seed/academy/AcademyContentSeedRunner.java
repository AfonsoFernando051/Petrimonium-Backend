package com.jf.PetApp.infrastructure.seed.academy;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.jf.PetApp.infrastructure.repository.academy.AcademyChoiceQuestionOptionJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyChoiceQuestionOptionTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyDomainJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyDomainTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonStepJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonStepTakeawayJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonPortfolioConceptJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonStepTakeawayTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonStepTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyLessonTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyModulePrerequisiteJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademyModuleTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademySchoolJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademySchoolPrerequisiteJpaRepository;
import com.jf.PetApp.infrastructure.repository.academy.AcademySchoolTranslationJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LearningLessonJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LearningModuleJpaRepository;
import com.jf.PetApp.infrastructure.seed.academy.model.DomainSeedDto;
import com.jf.PetApp.infrastructure.seed.academy.model.DomainsFileDto;
import com.jf.PetApp.infrastructure.seed.academy.model.LessonSeedDto;
import com.jf.PetApp.infrastructure.seed.academy.model.LessonTextDto;
import com.jf.PetApp.infrastructure.seed.academy.model.LocalizedTextDto;
import com.jf.PetApp.infrastructure.seed.academy.model.ModuleSeedDto;
import com.jf.PetApp.infrastructure.seed.academy.model.SchoolSeedDto;
import com.jf.PetApp.infrastructure.seed.academy.model.StepSeedDto;
import com.jf.PetApp.infrastructure.seed.academy.model.StepTextDto;

/**
 * Loads the Academy curriculum content from the JSON files under
 * {@code src/main/resources/academy-content/} and upserts it into the
 * database on every application boot.
 *
 * <p>This is the project's content authoring pipeline for the Academy,
 * replacing the old approach (hardcoded {@code INSERT}s in Flyway
 * migrations, see {@code V4}/{@code V9}) — content changes are reviewed as a
 * JSON diff, not a SQL migration.
 *
 * <p>Idempotent by construction: {@code academy_domains}/{@code
 * academy_schools}/{@code learning_modules}/{@code learning_lessons} have a
 * permanent, natural id — upserted via {@code findById().orElseGet(new)} +
 * {@code save()}. Every other table here (translations, prerequisites,
 * steps, options, takeaways) has no id shared with user progress, so it's
 * simply deleted for its parent and reinserted every boot — cheap at this
 * content's size, and it can never leave a stale row behind when an author
 * removes a step/option/translation from the JSON.
 *
 * <p>Ids are never deleted or renamed here — discontinuing a school/module
 * is done by setting {@code contentAvailable: false} in its JSON, not by
 * removing its entry, since {@code lesson_progress}/{@code xp_events} key
 * off these ids permanently. See {@code academy-content/README.md}.
 */
@Component
public class AcademyContentSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AcademyContentSeedRunner.class);
    private static final String CONTENT_ROOT = "classpath:academy-content";

    // A plain, locally-owned instance rather than an injected Spring bean:
    // Spring Boot 4's autoconfigured JSON bean is Jackson 3's
    // tools.jackson.databind.ObjectMapper, not this (Jackson 2) type — this
    // com.fasterxml.jackson.databind.ObjectMapper class only reaches the
    // classpath as a runtime dependency of jjwt-jackson (JWT signing), with
    // no Spring bean of its own. Same pattern already used by
    // OnboardingControllerTest.
    private final ObjectMapper objectMapper = new ObjectMapper();
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
    private final CacheManager cacheManager;

    public AcademyContentSeedRunner(
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
            AcademyLessonStepTakeawayTranslationJpaRepository takeawayTranslationRepository,
            CacheManager cacheManager) {
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
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        DomainsFileDto domainsFile = readDomains();
        List<SchoolSeedDto> schools = readSchools();

        for (DomainSeedDto domain : domainsFile.domains()) {
            upsertDomain(domain);
        }

        for (SchoolSeedDto school : schools) {
            upsertSchool(school);
            for (ModuleSeedDto module : school.modules()) {
                upsertModule(module, school.schoolId());
                for (LessonSeedDto lesson : module.lessons()) {
                    upsertLesson(lesson, module.moduleId());
                    reconcileSteps(lesson);
                    reconcileLessonPortfolioConcepts(lesson.lessonId(), lesson.portfolioConceptsOrEmpty());
                }
            }
        }

        // Prerequisites are resolved last, once every school/module referenced by an
        // id has already been upserted above.
        for (SchoolSeedDto school : schools) {
            reconcileSchoolPrerequisites(school.schoolId(), school.prerequisites());
            for (ModuleSeedDto module : school.modules()) {
                reconcileModulePrerequisites(module.moduleId(), module.prerequisites());
            }
        }

        // The embedded server can receive a request while ApplicationRunners
        // are still seeding. If that early request cached an empty catalog,
        // discard it only after the complete curriculum is committed.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Cache catalogCache = cacheManager.getCache(CacheConfig.ACADEMY_CATALOG_CACHE);
                if (catalogCache != null) {
                    catalogCache.clear();
                }
            }
        });
        log.info("Academy content seeded: {} domains, {} schools", domainsFile.domains().size(), schools.size());
    }

    private DomainsFileDto readDomains() throws IOException {
        Resource resource = new PathMatchingResourcePatternResolver().getResource(CONTENT_ROOT + "/domains.json");
        return objectMapper.readValue(resource.getInputStream(), DomainsFileDto.class);
    }

    private List<SchoolSeedDto> readSchools() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(CONTENT_ROOT + "/schools/**/*.json");
        List<SchoolSeedDto> schools = new ArrayList<>();
        for (Resource resource : resources) {
            schools.add(objectMapper.readValue(resource.getInputStream(), SchoolSeedDto.class));
        }
        return schools;
    }

  private void upsertDomain(DomainSeedDto dto) {
    AcademyDomainJpaEntity entity = domainRepository
        .findById(dto.domainId())
        .orElseGet(AcademyDomainJpaEntity::new);

    entity.setDomainId(dto.domainId());
    entity.setOrderIndex(dto.order());
    entity.setIconKey(dto.iconKey());

    domainRepository.save(entity);

        for (Map.Entry<String, LocalizedTextDto> translation :
                dto.translations().entrySet()) {

            String lang = translation.getKey();
            LocalizedTextDto localized = translation.getValue();

            AcademyDomainTranslationJpaEntity t =
                domainTranslationRepository
                    .findByDomainIdAndLang(dto.domainId(), lang)
                    .orElseGet(AcademyDomainTranslationJpaEntity::new);

            t.setDomainId(dto.domainId());
            t.setLang(lang);
            t.setTitle(localized.title());
            t.setDescription(localized.description());

            domainTranslationRepository.save(t);
        }
    }

    private void upsertSchool(SchoolSeedDto dto) {
        AcademySchoolJpaEntity entity = schoolRepository
            .findById(dto.schoolId())
            .orElseGet(AcademySchoolJpaEntity::new);

        entity.setSchoolId(dto.schoolId());
        entity.setDomainId(dto.domainId());
        entity.setOrderIndex(dto.order());
        entity.setIconKey(dto.iconKey());
        entity.setContentAvailable(dto.contentAvailable());

        schoolRepository.save(entity);

        for (Map.Entry<String, LocalizedTextDto> translation :
                dto.translations().entrySet()) {

            String lang = translation.getKey();
            LocalizedTextDto localized = translation.getValue();

            AcademySchoolTranslationJpaEntity t =
                schoolTranslationRepository
                    .findBySchoolIdAndLang(dto.schoolId(), lang)
                    .orElseGet(AcademySchoolTranslationJpaEntity::new);

            t.setSchoolId(dto.schoolId());
            t.setLang(lang);
            t.setTitle(localized.title());
            t.setDescription(localized.description());

            schoolTranslationRepository.save(t);
        }
    }

    private void upsertModule(ModuleSeedDto dto, String schoolId) {
        LearningModuleJpaEntity entity = moduleRepository
                .findById(dto.moduleId())
                .orElseGet(LearningModuleJpaEntity::new);

            entity.setModuleId(dto.moduleId());
            entity.setXpReward(dto.xpReward());
            entity.setModuleOrder(dto.order());
            entity.setLessonCount(dto.lessons().size());
            entity.setSchoolId(schoolId);
            entity.setIconKey(dto.iconKey());
            entity.setContentAvailable(dto.contentAvailable());
            entity.setDifficulty(dto.difficulty());

            moduleRepository.save(entity);

            for (Map.Entry<String, LocalizedTextDto> translation :
                    dto.translations().entrySet()) {

                String lang = translation.getKey();
                LocalizedTextDto localized = translation.getValue();

                AcademyModuleTranslationJpaEntity t =
                    moduleTranslationRepository
                        .findByModuleIdAndLang(dto.moduleId(), lang)
                        .orElseGet(AcademyModuleTranslationJpaEntity::new);

                t.setModuleId(dto.moduleId());
                t.setLang(lang);
                t.setTitle(localized.title());
                t.setDescription(localized.description());

                moduleTranslationRepository.save(t);
            }
    }

    private void upsertLesson(LessonSeedDto dto, String moduleId) {
        LearningLessonJpaEntity entity = lessonRepository
            .findById(dto.lessonId())
            .orElseGet(LearningLessonJpaEntity::new);

        entity.setLessonId(dto.lessonId());
        entity.setModuleId(moduleId);
        entity.setXpReward(dto.xpReward());
        entity.setLessonOrder(dto.order());
        entity.setCompetency(dto.competency());
        entity.setEstimatedMinutes(dto.estimatedMinutes());
        entity.setJurisdiction(dto.jurisdiction());
        entity.setEffectiveDate(parseDate(dto.effectiveDate()));
        entity.setLastVerifiedAt(parseDate(dto.lastVerifiedAt()));
        entity.setSource(dto.source());

        lessonRepository.save(entity);

        for (Map.Entry<String, LessonTextDto> translation :
                dto.translations().entrySet()) {

            String lang = translation.getKey();
            LessonTextDto localized = translation.getValue();

            AcademyLessonTranslationJpaEntity t =
                lessonTranslationRepository
                    .findByLessonIdAndLang(dto.lessonId(), lang)
                    .orElseGet(AcademyLessonTranslationJpaEntity::new);

            t.setLessonId(dto.lessonId());
            t.setLang(lang);
            t.setTitle(localized.title());
            t.setLearningObjective(localized.learningObjective());

            lessonTranslationRepository.save(t);
        }
}

    /** {@code null}-safe: most lessons carry no Taxation-only date field at all — see DECISION-025. */
    private static LocalDate parseDate(String isoDate) {
        return isoDate == null ? null : LocalDate.parse(isoDate);
    }

    private void reconcileSteps(LessonSeedDto lesson) {
        for (AcademyLessonStepJpaEntity existing :
                stepRepository.findByLessonIdOrderByStepOrderAsc(lesson.lessonId())) {
            deleteStepChildren(existing.getId());
        }

        // Ensure all child records are physically deleted before deleting/recreating
        // the parent steps. This prevents delete/insert ordering conflicts in PostgreSQL.
        stepTranslationRepository.flush();
        optionTranslationRepository.flush();
        optionRepository.flush();
        takeawayTranslationRepository.flush();
        takeawayRepository.flush();

        stepRepository.deleteByLessonId(lesson.lessonId());
        stepRepository.flush();

        for (StepSeedDto stepDto : lesson.steps()) {
            AcademyLessonStepJpaEntity step =
                    new AcademyLessonStepJpaEntity();

            step.setLessonId(lesson.lessonId());
            step.setStepOrder(stepDto.order());
            step.setStepType(stepDto.type().toUpperCase(Locale.ROOT));
            step.setFraming(
                    stepDto.framing() == null
                            ? null
                            : stepDto.framing().toUpperCase(Locale.ROOT)
            );
            step.setCorrectOptionIndex(stepDto.correctIndex());

            step = stepRepository.save(step);

            for (Map.Entry<String, StepTextDto> translation :
                    stepDto.translations().entrySet()) {

                StepTextDto text = translation.getValue();

                AcademyLessonStepTranslationJpaEntity t =
                        new AcademyLessonStepTranslationJpaEntity();

                t.setStepId(step.getId());
                t.setLang(translation.getKey());
                t.setTitle(text.title());
                t.setBody(text.body());
                t.setPrompt(text.prompt());
                t.setExplanation(text.explanation());

                stepTranslationRepository.save(t);
            }

            if ("choice_question".equalsIgnoreCase(stepDto.type())) {
                reconcileOptions(step.getId(), stepDto.translations());
            }

            if ("summary".equalsIgnoreCase(stepDto.type())) {
                reconcileTakeaways(step.getId(), stepDto.translations());
            }
        }
    }

    /**
     * Explicitly deletes everything under one step (translations, options +
     * their translations, takeaways + their translations) — not left to
     * {@code ON DELETE CASCADE}, since these are plain unmapped
     * foreign-key columns, not JPA relationships, so nothing cascades them
     * automatically in every environment this runs in (see
     * {@link com.jf.PetApp.infrastructure.repository.academy.AcademyLessonStepJpaRepository#deleteByLessonId}).
     */
    private void deleteStepChildren(Long stepId) {
        stepTranslationRepository.deleteByStepId(stepId);

        for (AcademyChoiceQuestionOptionJpaEntity option : optionRepository.findByStepIdOrderByPositionAsc(stepId)) {
            optionTranslationRepository.deleteByOptionId(option.getId());
        }
        optionRepository.deleteByStepId(stepId);

        for (AcademyLessonStepTakeawayJpaEntity takeaway : takeawayRepository.findByStepIdOrderByPositionAsc(stepId)) {
            takeawayTranslationRepository.deleteByTakeawayId(takeaway.getId());
        }
        takeawayRepository.deleteByStepId(stepId);
    }

    /**
     * Choice-question options are per-language lists; position N across every
     * language refers to the same option. The number of positions to create is
     * the longest of the per-language option lists (they're expected to match
     * exactly — an author error would show up as a missing translation, not a
     * silently dropped option).
     */
    private void reconcileOptions(Long stepId, Map<String, StepTextDto> translations) {
        int optionCount = translations.values().stream()
                .mapToInt(t -> t.options() == null ? 0 : t.options().size())
                .max()
                .orElse(0);

        for (int position = 0; position < optionCount; position++) {
            AcademyChoiceQuestionOptionJpaEntity option = new AcademyChoiceQuestionOptionJpaEntity();
            option.setStepId(stepId);
            option.setPosition(position);
            option = optionRepository.save(option);

            for (Map.Entry<String, StepTextDto> translation : translations.entrySet()) {
                List<String> options = translation.getValue().options();
                if (options == null || position >= options.size()) {
                    continue;
                }
                AcademyChoiceQuestionOptionTranslationJpaEntity t = new AcademyChoiceQuestionOptionTranslationJpaEntity();
                t.setOptionId(option.getId());
                t.setLang(translation.getKey());
                t.setOptionText(options.get(position));
                optionTranslationRepository.save(t);
            }
        }
    }

    private void reconcileTakeaways(Long stepId, Map<String, StepTextDto> translations) {
        int takeawayCount = translations.values().stream()
                .mapToInt(t -> t.takeaways() == null ? 0 : t.takeaways().size())
                .max()
                .orElse(0);

        for (int position = 0; position < takeawayCount; position++) {
            AcademyLessonStepTakeawayJpaEntity takeaway = new AcademyLessonStepTakeawayJpaEntity();
            takeaway.setStepId(stepId);
            takeaway.setPosition(position);
            takeaway = takeawayRepository.save(takeaway);

            for (Map.Entry<String, StepTextDto> translation : translations.entrySet()) {
                List<String> takeaways = translation.getValue().takeaways();
                if (takeaways == null || position >= takeaways.size()) {
                    continue;
                }
                AcademyLessonStepTakeawayTranslationJpaEntity t = new AcademyLessonStepTakeawayTranslationJpaEntity();
                t.setTakeawayId(takeaway.getId());
                t.setLang(translation.getKey());
                t.setTakeawayText(takeaways.get(position));
                takeawayTranslationRepository.save(t);
            }
        }
    }

    private void reconcileSchoolPrerequisites(
        String schoolId,
        List<String> prerequisites) {

        schoolPrerequisiteRepository.deleteBySchoolId(schoolId);
        schoolPrerequisiteRepository.flush();

        for (String prerequisiteId : prerequisites) {
            AcademySchoolPrerequisiteJpaEntity e =
                    new AcademySchoolPrerequisiteJpaEntity();

            e.setSchoolId(schoolId);
            e.setPrerequisiteSchoolId(prerequisiteId);

            schoolPrerequisiteRepository.save(e);
        }
    }

    private void reconcileModulePrerequisites(
        String moduleId,
        List<String> prerequisites) {

        modulePrerequisiteRepository.deleteByModuleId(moduleId);
        modulePrerequisiteRepository.flush();

        for (String prerequisiteId : prerequisites) {
            AcademyModulePrerequisiteJpaEntity e =
                    new AcademyModulePrerequisiteJpaEntity();

            e.setModuleId(moduleId);
            e.setPrerequisiteModuleId(prerequisiteId);

            modulePrerequisiteRepository.save(e);
        }
    }

    private void reconcileLessonPortfolioConcepts(
        String lessonId,
        List<String> conceptIds) {

        lessonPortfolioConceptRepository.deleteByLessonId(lessonId);
        lessonPortfolioConceptRepository.flush();

        for (String conceptId : conceptIds) {
            AcademyLessonPortfolioConceptJpaEntity e =
                    new AcademyLessonPortfolioConceptJpaEntity();

            e.setLessonId(lessonId);
            e.setConceptId(conceptId);

            lessonPortfolioConceptRepository.save(e);
        }
    }
}
