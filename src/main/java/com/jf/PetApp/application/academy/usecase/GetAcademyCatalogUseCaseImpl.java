package com.jf.PetApp.application.academy.usecase;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.port.AcademyCatalogQueryPort;

@Service
public class GetAcademyCatalogUseCaseImpl implements GetAcademyCatalogUseCase {

    private final AcademyCatalogQueryPort academyCatalogQueryPort;

    public GetAcademyCatalogUseCaseImpl(AcademyCatalogQueryPort academyCatalogQueryPort) {
        this.academyCatalogQueryPort = academyCatalogQueryPort;
    }

    @Override
    public AcademyCatalogResult execute(String lang) {
        return academyCatalogQueryPort.getCatalog(lang);
    }
}
