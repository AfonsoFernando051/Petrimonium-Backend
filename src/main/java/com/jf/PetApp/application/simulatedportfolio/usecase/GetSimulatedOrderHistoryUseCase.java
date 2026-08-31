package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;

import java.util.List;

public interface GetSimulatedOrderHistoryUseCase {
    List<SimulatedOrderDTO> execute(String email);
}
