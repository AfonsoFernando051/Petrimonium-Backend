package com.jf.PetApp.application.investment.usecase;

import java.util.List;

public interface ConfigureInvestmentsUseCase {

    /**
     * Replaces the caller's whole portfolio with {@code investments}.
     *
     * @param confirmReplace acknowledgment that a submission holding fewer lots
     *        than the user currently owns is a deliberate replacement. When
     *        {@code false}, such a submission is rejected with
     *        {@link com.jf.PetApp.application.investment.exception.DestructivePortfolioReplaceException}
     *        instead of silently destroying real investments.
     */
    void execute(String email, List<ConfigureInvestmentCommand> investments, boolean confirmReplace);
}
