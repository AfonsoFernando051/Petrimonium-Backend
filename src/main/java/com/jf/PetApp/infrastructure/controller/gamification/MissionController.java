package com.jf.PetApp.infrastructure.controller.gamification;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.gamification.dto.MissionEvaluationResult;
import com.jf.PetApp.application.gamification.dto.MissionStatusDTO;
import com.jf.PetApp.application.gamification.usecase.EvaluateMissionsUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/missions")
public class MissionController {

    private final EvaluateMissionsUseCase evaluateMissionsUseCase;

    public MissionController(EvaluateMissionsUseCase evaluateMissionsUseCase) {
        this.evaluateMissionsUseCase = evaluateMissionsUseCase;
    }

    @GetMapping
    public ResponseEntity<MissionsResponseDTO> getMissions() {
        String email = SecurityUtils.getCurrentUserEmail();
        MissionEvaluationResult result = evaluateMissionsUseCase.execute(email);
        return ResponseEntity.ok(MissionsResponseDTO.from(result));
    }

    public record MissionsResponseDTO(
            List<MissionStatusDTO> missions, Set<String> newlyCompletedCodes, int missionXpTotal) {
        static MissionsResponseDTO from(MissionEvaluationResult r) {
            return new MissionsResponseDTO(r.missions(), r.newlyCompletedCodes(), r.missionXpTotal());
        }
    }
}
