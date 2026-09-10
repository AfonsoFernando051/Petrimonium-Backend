package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.InvestmentDTO;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Investment;

import org.springframework.stereotype.Service;

@Service
public class CreateInvestmentLotUseCaseImpl implements CreateInvestmentLotUseCase {

    private final InvestmentRepositoryPort investmentRepositoryPort;
    private final UserRepository userRepository;

    public CreateInvestmentLotUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort, UserRepository userRepository) {
        this.investmentRepositoryPort = investmentRepositoryPort;
        this.userRepository = userRepository;
    }

    @Override
    public InvestmentDTO execute(String email, InvestmentLotCommand command) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        Investment created = investmentRepositoryPort.create(email, new Investment(
                null, email, command.name(), command.quantity(), command.purchasePrice(), command.purchaseDate(), command.type()));

        return toDTO(created);
    }

    private InvestmentDTO toDTO(Investment investment) {
        return new InvestmentDTO(
                investment.id(), investment.name(), investment.quantity(),
                investment.purchasePrice(), investment.purchaseDate(), investment.type());
    }
}
