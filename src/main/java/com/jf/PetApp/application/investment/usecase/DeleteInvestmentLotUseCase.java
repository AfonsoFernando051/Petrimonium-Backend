package com.jf.PetApp.application.investment.usecase;

/**
 * Removes one lot. Throws
 * {@link com.jf.PetApp.application.common.exception.ResourceNotFoundException} when the id
 * doesn't exist or isn't the caller's, same scoping as {@link UpdateInvestmentLotUseCase}. Unlike
 * {@link ConfigureInvestmentsUseCase}'s shrink guard, no confirmation is required: the verb and
 * the id already say which lot the caller means to remove.
 */
public interface DeleteInvestmentLotUseCase {

    void execute(String email, Integer id);
}
