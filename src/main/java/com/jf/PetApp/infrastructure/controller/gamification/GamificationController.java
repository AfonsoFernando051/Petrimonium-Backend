package com.jf.PetApp.infrastructure.controller.gamification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.gamification.dto.GamificationSummaryResult;
import com.jf.PetApp.application.gamification.usecase.GetGamificationSummaryUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {

    private final GetGamificationSummaryUseCase getGamificationSummaryUseCase;

    public GamificationController(GetGamificationSummaryUseCase getGamificationSummaryUseCase) {
        this.getGamificationSummaryUseCase = getGamificationSummaryUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<GamificationSummaryResponseDTO> getSummary() {
        String email = SecurityUtils.getCurrentUserEmail();
        GamificationSummaryResult result = getGamificationSummaryUseCase.execute(email);
        return ResponseEntity.ok(new GamificationSummaryResponseDTO(
                result.totalXp(), result.level(), result.xpIntoLevel(), result.xpForNextLevel(),
                result.currentStreak(), result.longestStreak()));
    }

    public record GamificationSummaryResponseDTO(
            int totalXp, int level, int xpIntoLevel, int xpForNextLevel, int currentStreak, int longestStreak) {
    }
}
