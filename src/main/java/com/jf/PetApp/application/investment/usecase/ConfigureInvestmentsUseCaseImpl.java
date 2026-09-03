package com.jf.PetApp.application.investment.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.exception.DestructivePortfolioReplaceException;
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
    public void execute(String email, List<ConfigureInvestmentCommand> commands, boolean confirmReplace) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        guardAgainstUnconfirmedShrink(email, commands, confirmReplace);

        List<Investment> investments = commands.stream()
                .map(c -> new Investment(null, email, c.name(), c.quantity(), c.purchasePrice(), c.purchaseDate(), c.type()))
                .toList();

        investmentRepositoryPort.deleteByUserEmail(email);
        investmentRepositoryPort.saveAll(email, investments);
    }

    /**
     * Scoped to <em>reductions</em> on purpose: adding assets is the common path and is
     * never destructive, so a client too old to send {@code confirmReplace} keeps working
     * for it. Only a submission that would leave the user with fewer lots than they hold
     * today has to say it means it.
     */
    private void guardAgainstUnconfirmedShrink(String email, List<ConfigureInvestmentCommand> commands, boolean confirmReplace) {
        if (confirmReplace) {
            return;
        }
        int currentLotCount = investmentRepositoryPort.findByUserEmail(email).size();
        if (commands.size() < currentLotCount) {
            throw new DestructivePortfolioReplaceException(currentLotCount, commands.size());
        }
    }
}
