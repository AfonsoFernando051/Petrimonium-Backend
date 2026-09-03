package com.jf.PetApp.application.investment.usecase;

import java.util.List;

public interface ConfigureInvestmentsUseCase {
    /**
     * Replaces the user's whole portfolio with {@code investments}.
     *
     * @param confirmReplace the caller explicitly acknowledges that existing
     *                        lots may be removed. Required only when the
     *                        submission would end up with fewer lots than the
     *                        user holds today — see
     *                        {@code DestructivePortfolioReplaceException}.
     */
    void execute(String email, List<ConfigureInvestmentCommand> investments, boolean confirmReplace);
}
