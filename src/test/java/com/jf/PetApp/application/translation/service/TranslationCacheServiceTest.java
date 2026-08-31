package com.jf.PetApp.application.translation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jf.PetApp.application.translation.port.TranslationClient;

@ExtendWith(MockitoExtension.class)
class TranslationCacheServiceTest {

    @Mock
    private TranslationClient translationClient;

    private TranslationCacheService service;

    @BeforeEach
    void setUp() {
        service = new TranslationCacheService(translationClient);
    }

    @Test
    void translate_TargetLangIsSource_ReturnsOriginalTextWithoutCallingClient() {
        String result = service.translate("Olá mundo", "pt");

        assertEquals("Olá mundo", result);
        verify(translationClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_TargetLangIsNull_ReturnsOriginalTextWithoutCallingClient() {
        String result = service.translate("Olá mundo", null);

        assertEquals("Olá mundo", result);
        verify(translationClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_UnsupportedLanguage_ReturnsOriginalTextWithoutCallingClient() {
        String result = service.translate("Olá mundo", "fr");

        assertEquals("Olá mundo", result);
        verify(translationClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translate_SupportedLanguageFirstCall_CallsClientAndReturnsTranslation() {
        when(translationClient.translate("Olá mundo", "pt", "en"))
                .thenReturn(Optional.of("Hello world"));

        String result = service.translate("Olá mundo", "en");

        assertEquals("Hello world", result);
        verify(translationClient, times(1)).translate("Olá mundo", "pt", "en");
    }

    @Test
    void translate_SecondCallSameTextAndLang_UsesCacheAndDoesNotCallClientAgain() {
        when(translationClient.translate("Olá mundo", "pt", "en"))
                .thenReturn(Optional.of("Hello world"));

        service.translate("Olá mundo", "en");
        String secondResult = service.translate("Olá mundo", "en");

        assertEquals("Hello world", secondResult);
        verify(translationClient, times(1)).translate("Olá mundo", "pt", "en");
    }

    @Test
    void translate_ClientReturnsEmpty_FallsBackToOriginalTextAndCachesTheFallback() {
        when(translationClient.translate("Olá mundo", "pt", "es"))
                .thenReturn(Optional.empty());

        String first = service.translate("Olá mundo", "es");
        String second = service.translate("Olá mundo", "es");

        assertEquals("Olá mundo", first);
        assertEquals("Olá mundo", second);
        // Fallback result is cached too, so the client is still only hit once.
        verify(translationClient, times(1)).translate("Olá mundo", "pt", "es");
    }

    @Test
    void translate_SameTextDifferentTargetLanguages_CachedIndependently() {
        when(translationClient.translate("Olá mundo", "pt", "en"))
                .thenReturn(Optional.of("Hello world"));
        when(translationClient.translate("Olá mundo", "pt", "es"))
                .thenReturn(Optional.of("Hola mundo"));

        String en = service.translate("Olá mundo", "en");
        String es = service.translate("Olá mundo", "es");

        assertEquals("Hello world", en);
        assertEquals("Hola mundo", es);
        verify(translationClient, times(1)).translate("Olá mundo", "pt", "en");
        verify(translationClient, times(1)).translate("Olá mundo", "pt", "es");
    }
}
