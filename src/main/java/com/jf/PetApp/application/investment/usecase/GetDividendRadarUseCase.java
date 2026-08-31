package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.DividendRadarResponseDTO;

public interface GetDividendRadarUseCase {
    DividendRadarResponseDTO execute(String email);
}
