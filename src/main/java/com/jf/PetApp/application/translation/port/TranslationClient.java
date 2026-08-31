package com.jf.PetApp.application.translation.port;

import java.util.Optional;

public interface TranslationClient {
    /**
     * Translates text between two language codes (e.g. "pt", "en", "es").
     * Returns empty when the provider is unavailable so callers can fall
     * back to the original text instead of failing the request.
     */
    Optional<String> translate(String text, String sourceLang, String targetLang);
}
