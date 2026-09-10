package com.jf.PetApp.application.health.usecase;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.dto.HealthCommands.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CreateHealthPurchaseUseCase {
    PurchaseWithInstallments execute(String email, long cardId, PurchaseInput input);
}
