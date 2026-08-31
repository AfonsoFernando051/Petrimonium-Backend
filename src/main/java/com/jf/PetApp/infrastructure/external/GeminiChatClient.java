package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import com.jf.PetApp.application.mentor.port.MentorChatPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Mentor's fallback provider — {@link MentorChatFallbackClient} tries
 * {@link AnthropicChatClient} first and only calls this if that fails.
 */
@Service
public class GeminiChatClient implements MentorChatPort {

    private final RestTemplate restTemplate;

    @Value("${api.gemini.key:}")
    private String apiKey;

    @Value("${api.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${api.gemini.baseUrl:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    public GeminiChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String generateReply(String systemPrompt, List<MentorTurnDTO> history, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("api.gemini.key is not configured");
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            for (MentorTurnDTO turn : history) {
                contents.add(toContent(turn.role(), turn.text()));
            }
        }
        contents.add(toContent("user", userMessage));

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        body.put("systemInstruction", systemInstruction);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // The key travels as a header, never in the URL — a URL is far more likely to end
        // up verbatim in logs, error messages, proxies, or browser/HTTP-client history.
        headers.set("x-goog-api-key", apiKey);

        String url = String.format("%s/models/%s:generateContent", baseUrl, model);

        @SuppressWarnings("rawtypes")
        Map response;
        try {
            response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (RestClientException e) {
            // RestTemplate's exception messages already include the request URL; since the
            // key is now header-only, the URL is safe, but keep it explicit that nothing
            // header-related (i.e. the key) is ever part of this message.
            throw new IllegalStateException("Gemini request failed: " + e.getClass().getSimpleName(), e);
        }

        return extractReply(response);
    }

    private Map<String, Object> toContent(String role, String text) {
        String geminiRole = "mentor".equalsIgnoreCase(role) ? "model" : "user";
        return Map.of(
                "role", geminiRole,
                "parts", List.of(Map.of("text", text))
        );
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<?, ?> response) {
        if (response == null) {
            throw new IllegalStateException("Empty response from Gemini");
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates returned by Gemini");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw new IllegalStateException("No content returned by Gemini");
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("No parts returned by Gemini");
        }
        Object text = parts.get(0).get("text");
        if (!(text instanceof String textStr) || textStr.isBlank()) {
            throw new IllegalStateException("Empty text returned by Gemini");
        }
        return textStr;
    }
}
