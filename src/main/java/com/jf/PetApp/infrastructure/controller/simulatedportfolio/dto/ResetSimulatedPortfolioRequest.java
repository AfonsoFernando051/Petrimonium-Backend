package com.jf.PetApp.infrastructure.controller.simulatedportfolio.dto;

import jakarta.validation.constraints.AssertTrue;

public record ResetSimulatedPortfolioRequest(
        @AssertTrue(message = "confirm must be true to reset the simulated portfolio") boolean confirm
) {
}
