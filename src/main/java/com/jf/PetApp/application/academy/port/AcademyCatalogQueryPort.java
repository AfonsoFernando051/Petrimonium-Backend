package com.jf.PetApp.application.academy.port;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;

/** Read-only access to the Academy curriculum, resolved to one language. */
public interface AcademyCatalogQueryPort {

    AcademyCatalogResult getCatalog(String lang);
}
