package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentDTO;

/**
 * Edits one lot in place. Throws
 * {@link com.jf.PetApp.application.common.exception.ResourceNotFoundException} when the id
 * doesn't exist or isn't the caller's — there is no unscoped ownership pre-check to skip because
 * the scoped write itself is the only way in.
 */
public interface UpdateInvestmentLotUseCase {

    InvestmentDTO execute(String email, Integer id, InvestmentLotCommand command);
}
