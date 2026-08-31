package com.jf.PetApp.application.academy.usecase;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;

public interface GetAcademyCatalogUseCase {

    AcademyCatalogResult execute(String lang);
}
