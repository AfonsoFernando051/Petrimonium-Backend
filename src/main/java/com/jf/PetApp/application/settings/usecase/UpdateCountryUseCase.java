package com.jf.PetApp.application.settings.usecase;

public interface UpdateCountryUseCase {
    /** @return the user's persisted country code after the update. */
    String execute(String email, String countryCode);
}
