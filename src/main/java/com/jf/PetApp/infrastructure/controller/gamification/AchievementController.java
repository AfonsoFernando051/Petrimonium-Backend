package com.jf.PetApp.infrastructure.controller.gamification;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.gamification.dto.AchievementEvaluationResult;
import com.jf.PetApp.application.gamification.usecase.EvaluateAchievementsUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/achievements")
public class AchievementController {

    private final EvaluateAchievementsUseCase evaluateAchievementsUseCase;

    public AchievementController(EvaluateAchievementsUseCase evaluateAchievementsUseCase) {
        this.evaluateAchievementsUseCase = evaluateAchievementsUseCase;
    }

    @GetMapping
    public ResponseEntity<AchievementsResponseDTO> getAchievements() {
        String email = SecurityUtils.getCurrentUserEmail();
        AchievementEvaluationResult result = evaluateAchievementsUseCase.execute(email);
        return ResponseEntity.ok(AchievementsResponseDTO.from(result));
    }

    public record AchievementsResponseDTO(
            Map<String, Instant> unlockedAt,
            Set<String> newlyUnlockedCodes,
            int achievementXpTotal) {
        static AchievementsResponseDTO from(AchievementEvaluationResult r) {
            return new AchievementsResponseDTO(r.unlockedAt(), r.newlyUnlockedCodes(), r.achievementXpTotal());
        }
    }
}
