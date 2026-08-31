package com.jf.PetApp.application.investment.usecase;

import java.util.List;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;

public interface GetPortfolioAllocationUseCase {
    List<AllocationSliceDTO> execute(String email);
}
