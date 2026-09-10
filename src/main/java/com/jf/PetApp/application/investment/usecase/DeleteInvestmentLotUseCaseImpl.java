package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;

import org.springframework.stereotype.Service;

@Service
public class DeleteInvestmentLotUseCaseImpl implements DeleteInvestmentLotUseCase {

    private final InvestmentRepositoryPort investmentRepositoryPort;

    public DeleteInvestmentLotUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort) {
        this.investmentRepositoryPort = investmentRepositoryPort;
    }

    @Override
    public void execute(String email, Integer id) {
        // No separate ownership pre-check: the scoped delete below finds nothing (and throws the
        // same ResourceNotFoundException) when the lot isn't this user's.
        investmentRepositoryPort.delete(id, email);
    }
}
