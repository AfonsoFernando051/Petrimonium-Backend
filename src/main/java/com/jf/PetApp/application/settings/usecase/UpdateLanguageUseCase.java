package com.jf.PetApp.application.settings.usecase;

public interface UpdateLanguageUseCase {
    /** @return the user's persisted preferred language after the update. */
    String execute(String email, String language);
}
