package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentDTO;

/**
 * Appends a single lot to the caller's portfolio — never touches any
 * existing lot. Unlike {@link ConfigureInvestmentsUseCase}, this has no
 * destructive-replace ambiguity to guard against.
 */
public interface CreateInvestmentLotUseCase {

    InvestmentDTO execute(String email, InvestmentLotCommand command);
}
