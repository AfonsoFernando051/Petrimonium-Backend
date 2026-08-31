package com.jf.PetApp.infrastructure.controller.lab;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jf.PetApp.application.lab.dto.SimulatorCompletionResult;
import com.jf.PetApp.application.lab.dto.SimulatorProgressResult;
import com.jf.PetApp.application.lab.usecase.CompleteSimulatorUseCase;
import com.jf.PetApp.application.lab.usecase.GetSimulatorProgressUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

/**
 * Financial Lab simulator completion/progress (DECISION-037). A separate
 * {@code /api/v1/lab} surface from {@code LearningController} — lesson code
 * and its ports are lesson-shaped (catalog metadata, per-lesson progress
 * rows) and irrelevant here, where the {@code xp_events} ledger row itself
 * is the only record a completion needs.
 */
@RestController
@RequestMapping("/api/v1/lab")
public class LabController {

    private final CompleteSimulatorUseCase completeSimulatorUseCase;
    private final GetSimulatorProgressUseCase getSimulatorProgressUseCase;

    public LabController(
            CompleteSimulatorUseCase completeSimulatorUseCase,
            GetSimulatorProgressUseCase getSimulatorProgressUseCase) {
        this.completeSimulatorUseCase = completeSimulatorUseCase;
        this.getSimulatorProgressUseCase = getSimulatorProgressUseCase;
    }

    @PostMapping("/simulators/{simulatorId}/complete")
    public ResponseEntity<SimulatorCompletionResponseDTO> completeSimulator(@PathVariable String simulatorId) {
        String email = SecurityUtils.getCurrentUserEmail();
        SimulatorCompletionResult result = completeSimulatorUseCase.execute(email, simulatorId);
        return ResponseEntity.ok(SimulatorCompletionResponseDTO.from(result));
    }

    @GetMapping("/simulators/progress")
    public ResponseEntity<SimulatorProgressResponseDTO> getProgress() {
        String email = SecurityUtils.getCurrentUserEmail();
        SimulatorProgressResult result = getSimulatorProgressUseCase.execute(email);
        return ResponseEntity.ok(SimulatorProgressResponseDTO.from(result));
    }

    public record SimulatorCompletionResponseDTO(
            String simulatorId,
            boolean alreadyCompleted,
            int xpAwarded,
            int totalXp,
            int level,
            int xpIntoLevel,
            int xpForNextLevel) {
        static SimulatorCompletionResponseDTO from(SimulatorCompletionResult r) {
            return new SimulatorCompletionResponseDTO(
                    r.simulatorId(), r.alreadyCompleted(), r.xpAwarded(), r.totalXp(), r.level(),
                    r.xpIntoLevel(), r.xpForNextLevel());
        }
    }

    public record SimulatorProgressResponseDTO(
            Set<String> completedSimulatorIds,
            int totalXp,
            int level,
            int xpIntoLevel,
            int xpForNextLevel) {
        static SimulatorProgressResponseDTO from(SimulatorProgressResult r) {
            return new SimulatorProgressResponseDTO(
                    r.completedSimulatorIds(), r.totalXp(), r.level(), r.xpIntoLevel(), r.xpForNextLevel());
        }
    }
}
