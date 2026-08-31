package com.jf.PetApp.application.lab.usecase;

import com.jf.PetApp.application.lab.dto.SimulatorCompletionResult;

public interface CompleteSimulatorUseCase {
    SimulatorCompletionResult execute(String userEmail, String simulatorId);
}
