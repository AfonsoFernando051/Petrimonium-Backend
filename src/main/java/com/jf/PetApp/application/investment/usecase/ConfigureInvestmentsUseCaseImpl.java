package com.jf.PetApp.application.investment.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Investment;

@Service
public class ConfigureInvestmentsUseCaseImpl implements ConfigureInvestmentsUseCase {

    private final InvestmentRepositoryPort investmentRepositoryPort;
    private final UserRepository userRepository;

    public ConfigureInvestmentsUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort, UserRepository userRepository) {
        this.investmentRepositoryPort = investmentRepositoryPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void execute(String email, List<ConfigureInvestmentCommand> commands) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        List<Investment> investments = commands.stream()
                .map(c -> new Investment(null, email, c.name(), c.quantity(), c.purchasePrice(), c.purchaseDate(), c.type()))
                .toList();

        investmentRepositoryPort.deleteByUserEmail(email);
        investmentRepositoryPort.saveAll(email, investments);
    }
}
