package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import com.jf.PetApp.application.mentor.port.MentorChatPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The {@link MentorChatPort} bean the rest of the app actually depends on: tries
 * {@link AnthropicChatClient} (Claude, the Mentor's primary provider) and falls back to
 * {@link GeminiChatClient} if that throws — e.g. no API key configured, rate limit, outage.
 * {@code GetMentorReplyUseCaseImpl}'s own catch around this call is the last resort (a canned
 * reply) if both providers fail.
 */
@Service
@Primary
public class MentorChatFallbackClient implements MentorChatPort {

    private static final Logger log = LoggerFactory.getLogger(MentorChatFallbackClient.class);

    private final AnthropicChatClient anthropicChatClient;
    private final GeminiChatClient geminiChatClient;

    public MentorChatFallbackClient(AnthropicChatClient anthropicChatClient, GeminiChatClient geminiChatClient) {
        this.anthropicChatClient = anthropicChatClient;
        this.geminiChatClient = geminiChatClient;
    }

    @Override
    public String generateReply(String systemPrompt, List<MentorTurnDTO> history, String userMessage) {
        try {
            return anthropicChatClient.generateReply(systemPrompt, history, userMessage);
        } catch (Exception e) {
            // e.getMessage() is safe to log: neither client ever lets its API key reach an
            // exception message (see each client's own catch block).
            log.warn("Anthropic call failed, falling back to Gemini: {}", e.getMessage());
            return geminiChatClient.generateReply(systemPrompt, history, userMessage);
        }
    }
}
