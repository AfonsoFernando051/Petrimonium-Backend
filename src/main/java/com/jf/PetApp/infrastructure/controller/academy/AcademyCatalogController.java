package com.jf.PetApp.infrastructure.controller.academy;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonStepView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;
import com.jf.PetApp.application.academy.usecase.GetAcademyCatalogUseCase;

@RestController
@RequestMapping("/api/v1/academy")
public class AcademyCatalogController {

    private static final Set<String> SUPPORTED_LANGS = Set.of("pt", "en", "es");

    private final GetAcademyCatalogUseCase getAcademyCatalogUseCase;

    public AcademyCatalogController(GetAcademyCatalogUseCase getAcademyCatalogUseCase) {
        this.getAcademyCatalogUseCase = getAcademyCatalogUseCase;
    }

    @GetMapping("/catalog")
    public ResponseEntity<AcademyCatalogResponseDTO> getCatalog(@RequestParam(defaultValue = "pt") String lang) {
        if (!SUPPORTED_LANGS.contains(lang)) {
            throw new IllegalArgumentException("Unsupported lang: " + lang);
        }
        AcademyCatalogResult result = getAcademyCatalogUseCase.execute(lang);
        return ResponseEntity.ok(AcademyCatalogResponseDTO.from(result));
    }

    public record AcademyLessonStepResponseDTO(
            String type,
            String title,
            String body,
            String framing,
            List<String> options,
            Integer correctIndex,
            String prompt,
            String explanation,
            List<String> takeaways) {
        static AcademyLessonStepResponseDTO from(AcademyLessonStepView v) {
            return new AcademyLessonStepResponseDTO(
                    v.type(), v.title(), v.body(), v.framing(), v.options(), v.correctIndex(), v.prompt(),
                    v.explanation(), v.takeaways());
        }
    }

    public record AcademyLessonResponseDTO(
            String id,
            String moduleId,
            String title,
            String learningObjective,
            String competency,
            Integer estimatedMinutes,
            int order,
            int xpReward,
            List<String> portfolioConcepts,
            List<AcademyLessonStepResponseDTO> steps) {
        static AcademyLessonResponseDTO from(AcademyLessonView v) {
            return new AcademyLessonResponseDTO(
                    v.id(), v.moduleId(), v.title(), v.learningObjective(), v.competency(), v.estimatedMinutes(),
                    v.order(), v.xpReward(), v.portfolioConcepts(),
                    v.steps().stream().map(AcademyLessonStepResponseDTO::from).toList());
        }
    }

    public record AcademyModuleResponseDTO(
            String id,
            String schoolId,
            String title,
            String description,
            String iconKey,
            String difficulty,
            int order,
            List<String> lessonIds,
            List<String> prerequisites,
            boolean contentAvailable) {
        static AcademyModuleResponseDTO from(AcademyModuleView v) {
            return new AcademyModuleResponseDTO(
                    v.id(), v.schoolId(), v.title(), v.description(), v.iconKey(), v.difficulty(), v.order(),
                    v.lessonIds(), v.prerequisites(), v.contentAvailable());
        }
    }

    public record AcademySchoolResponseDTO(
            String id,
            String domainId,
            String title,
            String description,
            String iconKey,
            int order,
            List<String> prerequisites,
            boolean contentAvailable) {
        static AcademySchoolResponseDTO from(AcademySchoolView v) {
            return new AcademySchoolResponseDTO(
                    v.id(), v.domainId(), v.title(), v.description(), v.iconKey(), v.order(), v.prerequisites(),
                    v.contentAvailable());
        }
    }

    public record AcademyDomainResponseDTO(
            String id, String title, String description, String iconKey, int order, List<String> schoolIds) {
        static AcademyDomainResponseDTO from(AcademyDomainView v) {
            return new AcademyDomainResponseDTO(v.id(), v.title(), v.description(), v.iconKey(), v.order(), v.schoolIds());
        }
    }

    public record AcademyCatalogResponseDTO(
            List<AcademyDomainResponseDTO> domains,
            List<AcademySchoolResponseDTO> schools,
            List<AcademyModuleResponseDTO> modules,
            List<AcademyLessonResponseDTO> lessons) {
        static AcademyCatalogResponseDTO from(AcademyCatalogResult r) {
            return new AcademyCatalogResponseDTO(
                    r.domains().stream().map(AcademyDomainResponseDTO::from).toList(),
                    r.schools().stream().map(AcademySchoolResponseDTO::from).toList(),
                    r.modules().stream().map(AcademyModuleResponseDTO::from).toList(),
                    r.lessons().stream().map(AcademyLessonResponseDTO::from).toList());
        }
    }
}
