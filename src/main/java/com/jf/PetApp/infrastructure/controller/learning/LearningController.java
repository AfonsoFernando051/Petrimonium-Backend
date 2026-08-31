package com.jf.PetApp.infrastructure.controller.learning;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.learning.dto.LessonCompletionResult;
import com.jf.PetApp.application.learning.usecase.CompleteLessonUseCase;
import com.jf.PetApp.application.learning.usecase.GetLearningProgressUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final CompleteLessonUseCase completeLessonUseCase;
    private final GetLearningProgressUseCase getLearningProgressUseCase;

    public LearningController(CompleteLessonUseCase completeLessonUseCase, GetLearningProgressUseCase getLearningProgressUseCase) {
        this.completeLessonUseCase = completeLessonUseCase;
        this.getLearningProgressUseCase = getLearningProgressUseCase;
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<LessonCompletionResponseDTO> completeLesson(
            @PathVariable String lessonId,
            @RequestBody(required = false) CompleteLessonRequestDTO body) {
        String email = SecurityUtils.getCurrentUserEmail();
        boolean perfectFirstTry = body != null && body.perfectFirstTry();
        LessonCompletionResult result = completeLessonUseCase.execute(email, lessonId, perfectFirstTry);
        return ResponseEntity.ok(LessonCompletionResponseDTO.from(result));
    }

    @GetMapping("/progress")
    public ResponseEntity<LearningProgressResponseDTO> getProgress() {
        String email = SecurityUtils.getCurrentUserEmail();
        LearningProgressResult result = getLearningProgressUseCase.execute(email);
        return ResponseEntity.ok(LearningProgressResponseDTO.from(result));
    }

    /**
     * {@code perfectFirstTry} is optional — absent (or a body-less request,
     * from an older client build) is treated as {@code false} so lesson
     * completion and XP granting are entirely unaffected either way. See
     * DECISION-025.
     */
    public record CompleteLessonRequestDTO(boolean perfectFirstTry) {
    }

    public record LessonCompletionResponseDTO(
            String lessonId,
            boolean alreadyCompleted,
            int xpAwarded,
            boolean moduleCompleted,
            int moduleXpAwarded,
            int totalXp,
            int level,
            int xpIntoLevel,
            int xpForNextLevel) {
        static LessonCompletionResponseDTO from(LessonCompletionResult r) {
            return new LessonCompletionResponseDTO(
                    r.lessonId(), r.alreadyCompleted(), r.xpAwarded(), r.moduleCompleted(), r.moduleXpAwarded(),
                    r.totalXp(), r.level(), r.xpIntoLevel(), r.xpForNextLevel());
        }
    }

    public record LearningProgressResponseDTO(
            Set<String> completedLessonIds,
            Set<String> completedModuleIds,
            Set<String> perfectLessonIds,
            int totalXp,
            int level,
            int xpIntoLevel,
            int xpForNextLevel) {
        static LearningProgressResponseDTO from(LearningProgressResult r) {
            return new LearningProgressResponseDTO(
                    r.completedLessonIds(), r.completedModuleIds(), r.perfectLessonIds(), r.totalXp(), r.level(),
                    r.xpIntoLevel(), r.xpForNextLevel());
        }
    }
}
