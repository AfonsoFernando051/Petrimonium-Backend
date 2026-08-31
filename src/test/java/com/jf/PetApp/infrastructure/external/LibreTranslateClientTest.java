package com.jf.PetApp.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibreTranslateClientTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final LibreTranslateClient client = new LibreTranslateClient(restTemplate);

    @BeforeEach
    void configureClient() {
        ReflectionTestUtils.setField(client, "baseUrl", "https://libretranslate.com");
        ReflectionTestUtils.setField(client, "apiKey", "");
    }

    @Test
    void translate_WithNullText_ReturnsEmptyOptionalWithoutCallingTheApi() {
        Optional<String> result = client.translate(null, "pt", "en");

        assertTrue(result.isEmpty());
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void translate_WithBlankText_ReturnsTheBlankTextWithoutCallingTheApi() {
        Optional<String> result = client.translate("   ", "pt", "en");

        assertEquals(Optional.of("   "), result);
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void translate_WithSameSourceAndTargetLanguage_ReturnsTheOriginalTextWithoutCallingTheApi() {
        Optional<String> result = client.translate("hello", "en", "en");

        assertEquals(Optional.of("hello"), result);
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void translate_WithSuccessfulResponse_ReturnsTheTranslatedText() {
        Map<String, Object> response = Map.of("translatedText", "olá");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        Optional<String> result = client.translate("hello", "en", "pt");

        assertEquals(Optional.of("olá"), result);
    }

    @Test
    void translate_WithConfiguredApiKey_IncludesItInTheRequestBody() {
        ReflectionTestUtils.setField(client, "apiKey", "libre-key");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("translatedText", "olá"));

        client.translate("hello", "en", "pt");

        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) captor.getValue().getBody();
        assertEquals("libre-key", body.get("api_key"));
    }

    @Test
    void translate_WithoutApiKeyConfigured_OmitsItFromTheRequestBody() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("translatedText", "olá"));

        client.translate("hello", "en", "pt");

        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) captor.getValue().getBody();
        assertTrue(!body.containsKey("api_key"));
    }

    @Test
    void translate_WithMissingTranslatedTextField_ReturnsEmpty() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new HashMap<>());

        assertTrue(client.translate("hello", "en", "pt").isEmpty());
    }

    @Test
    void translate_WithNonStringTranslatedTextField_ReturnsEmpty() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("translatedText", 123));

        assertTrue(client.translate("hello", "en", "pt").isEmpty());
    }

    @Test
    void translate_WithNullResponse_ReturnsEmpty() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        assertTrue(client.translate("hello", "en", "pt").isEmpty());
    }

    @Test
    void translate_WhenExceptionOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertTrue(client.translate("hello", "en", "pt").isEmpty());
    }
}
