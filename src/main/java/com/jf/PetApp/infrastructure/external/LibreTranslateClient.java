package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.translation.port.TranslationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter for the LibreTranslate API (https://libretranslate.com).
 * Open-source and free-tier friendly, chosen over Google/DeepL/Azure to avoid
 * paid-account setup during the MVP phase (see docs/DECISIONS.md philosophy).
 */
@Service
public class LibreTranslateClient implements TranslationClient {

    private static final Logger log = LoggerFactory.getLogger(LibreTranslateClient.class);

    private final RestTemplate restTemplate;

    @Value("${api.libretranslate.url:https://libretranslate.com}")
    private String baseUrl;

    @Value("${api.libretranslate.key:}")
    private String apiKey;

    public LibreTranslateClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<String> translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank() || sourceLang.equals(targetLang)) {
            return Optional.ofNullable(text);
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put("q", text);
            body.put("source", sourceLang);
            body.put("target", targetLang);
            body.put("format", "text");
            if (apiKey != null && !apiKey.isBlank()) {
                body.put("api_key", apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            @SuppressWarnings("rawtypes")
            Map response = restTemplate.postForObject(
                    baseUrl + "/translate",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response != null && response.get("translatedText") instanceof String translated) {
                return Optional.of(translated);
            }
            return Optional.empty();
        } catch (Exception e) {
            // The API key travels in the POST body, not the URL, so this message is safe to log.
            log.warn("LibreTranslate call failed ({} -> {}): {}", sourceLang, targetLang, e.getMessage());
            return Optional.empty();
        }
    }
}
