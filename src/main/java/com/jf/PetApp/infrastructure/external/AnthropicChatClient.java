package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import com.jf.PetApp.application.mentor.port.MentorChatPort;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * The Mentor's primary chat provider (Claude, via the Anthropic Messages API). Plain
 * {@link RestTemplate}, matching the existing Brapi/LibreTranslate/Gemini client style rather
 * than pulling in the Anthropic SDK for one call shape. {@link MentorChatFallbackClient} is what
 * callers actually depend on — it falls back to {@link GeminiChatClient} if this throws.
 */
@Service
public class AnthropicChatClient implements MentorChatPort {

    private static final int MAX_TOKENS = 4096;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;

    @Value("${api.anthropic.key:}")
    private String apiKey;

    @Value("${api.anthropic.model:claude-opus-5}")
    private String model;

    @Value("${api.anthropic.baseUrl:https://api.anthropic.com/v1}")
    private String baseUrl;

    public AnthropicChatClient(@Qualifier("anthropicRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String generateReply(String systemPrompt, List<MentorTurnDTO> history, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("api.anthropic.key is not configured");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (history != null) {
            for (MentorTurnDTO turn : history) {
                messages.add(toMessage(turn.role(), turn.text()));
            }
        }
        messages.add(toMessage("user", userMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", MAX_TOKENS);
        body.put("system", systemPrompt);
        body.put("messages", messages);
        // Server-side fallback: if Opus 5's safety classifier declines a benign request, retry
        // it automatically on Anthropic's recommended substitute instead of surfacing a refusal.
        body.put("fallbacks", "default");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // The key travels as a header, never in the URL — same discipline as
        // GeminiChatClient's x-goog-api-key handling.
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        headers.set("anthropic-beta", "server-side-fallback-2026-07-01");

        String url = baseUrl + "/messages";

        @SuppressWarnings("rawtypes")
        Map response;
        try {
            response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (RestClientException e) {
            // RestTemplate's exception messages already include the request URL; since the key
            // is header-only, the URL is safe, but keep it explicit that nothing header-related
            // (i.e. the key) is ever part of this message.
            throw new IllegalStateException("Anthropic request failed: " + e.getClass().getSimpleName(), e);
        }

        return extractReply(response);
    }

    private Map<String, String> toMessage(String role, String text) {
        String anthropicRole = "mentor".equalsIgnoreCase(role) ? "assistant" : "user";
        return Map.of("role", anthropicRole, "content", text);
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<?, ?> response) {
        if (response == null) {
            throw new IllegalStateException("Empty response from Anthropic");
        }
        if ("refusal".equals(response.get("stop_reason"))) {
            // Only reachable if the server-side fallback above itself couldn't recover the
            // request — let the caller fall back to Gemini rather than surface a refusal.
            throw new IllegalStateException("Anthropic declined the request (stop_reason=refusal)");
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("No content returned by Anthropic");
        }
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> block : content) {
            // Thinking blocks (type "thinking") carry empty text under the default "omitted"
            // display and must be skipped, not concatenated, to avoid corrupting the reply.
            if ("text".equals(block.get("type")) && block.get("text") instanceof String blockText) {
                text.append(blockText);
            }
        }
        String reply = text.toString();
        if (reply.isBlank()) {
            throw new IllegalStateException("No text content returned by Anthropic");
        }
        return reply;
    }
}
