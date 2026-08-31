package com.jf.PetApp.application.investment.usecase;

import java.util.List;

public interface ConfigureInvestmentsUseCase {
    void execute(String email, List<ConfigureInvestmentCommand> investments);
}
