package com.jf.PetApp.application.translation.service;

import com.jf.PetApp.application.translation.port.TranslationClient;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates pt-BR content (the canonical source language for this app) into
 * the small set of languages the Settings screen exposes, caching results in
 * memory since the source content (onboarding questions) is static.
 * Falls back to the original pt-BR text if the translation provider fails,
 * so an external outage never breaks the onboarding flow.
 */
@Service
public class TranslationCacheService {

    public static final String SOURCE_LANGUAGE = "pt";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("pt", "en", "es");

    private final TranslationClient translationClient;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public TranslationCacheService(TranslationClient translationClient) {
        this.translationClient = translationClient;
    }

    public String translate(String text, String targetLang) {
        if (targetLang == null || SOURCE_LANGUAGE.equals(targetLang)) {
            return text;
        }
        if (!SUPPORTED_LANGUAGES.contains(targetLang)) {
            return text;
        }

        String cacheKey = targetLang + "::" + text;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String translated = translationClient.translate(text, SOURCE_LANGUAGE, targetLang).orElse(text);
        cache.put(cacheKey, translated);
        return translated;
    }
}
