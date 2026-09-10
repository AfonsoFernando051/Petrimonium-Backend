package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentDTO;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;

import org.springframework.stereotype.Service;

@Service
public class UpdateInvestmentLotUseCaseImpl implements UpdateInvestmentLotUseCase {

    private final InvestmentRepositoryPort investmentRepositoryPort;

    public UpdateInvestmentLotUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort) {
        this.investmentRepositoryPort = investmentRepositoryPort;
    }

    @Override
    public InvestmentDTO execute(String email, Integer id, InvestmentLotCommand command) {
        // No separate ownership pre-check: the scoped update below finds nothing (and throws the
        // same ResourceNotFoundException) when the lot isn't this user's.
        Investment updated = investmentRepositoryPort.update(id, email, new Investment(
                null, email, command.name(), command.quantity(), command.purchasePrice(), command.purchaseDate(), command.type()));

        return toDTO(updated);
    }

    private InvestmentDTO toDTO(Investment investment) {
        return new InvestmentDTO(
                investment.id(), investment.name(), investment.quantity(),
                investment.purchasePrice(), investment.purchaseDate(), investment.type());
    }
}
