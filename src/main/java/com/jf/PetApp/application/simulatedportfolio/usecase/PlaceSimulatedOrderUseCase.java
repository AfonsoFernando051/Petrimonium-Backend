package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;

public interface PlaceSimulatedOrderUseCase {
    SimulatedOrderDTO execute(String email, PlaceSimulatedOrderCommand command);
}
