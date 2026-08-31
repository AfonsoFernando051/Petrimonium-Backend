package com.jf.PetApp.application.lab.usecase;

import com.jf.PetApp.application.lab.dto.SimulatorProgressResult;

public interface GetSimulatorProgressUseCase {
    SimulatorProgressResult execute(String userEmail);
}
