package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MentorChatFallbackClientTest {

    @Mock
    private AnthropicChatClient anthropicChatClient;
    @Mock
    private GeminiChatClient geminiChatClient;

    private MentorChatFallbackClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new MentorChatFallbackClient(anthropicChatClient, geminiChatClient);
    }

    @Test
    void generateReply_WhenGeminiSucceeds_ReturnsItsReplyAndNeverCallsAnthropic() {
        when(geminiChatClient.generateReply(anyString(), any(), anyString())).thenReturn("Gemini reply");

        String reply = client.generateReply("system", List.of(), "hi");

        assertEquals("Gemini reply", reply);
        verify(anthropicChatClient, never()).generateReply(anyString(), any(), anyString());
    }

    @Test
    void generateReply_WhenGeminiThrows_FallsBackToAnthropicReply() {
        when(geminiChatClient.generateReply(anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("api.gemini.key is not configured"));
        when(anthropicChatClient.generateReply(anyString(), any(), anyString())).thenReturn("Claude reply");

        String reply = client.generateReply("system", List.of(), "hi");

        assertEquals("Claude reply", reply);
    }

    @Test
    void generateReply_WhenBothProvidersThrow_PropagatesAnthropicsException() {
        when(geminiChatClient.generateReply(anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("gemini failed"));
        when(anthropicChatClient.generateReply(anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("anthropic failed"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
        assertEquals("anthropic failed", thrown.getMessage());
    }

    @Test
    void generateReply_PassesTheSameArgumentsThroughToWhicheverProviderRuns() {
        List<MentorTurnDTO> history = List.of(new MentorTurnDTO("user", "earlier turn"));
        when(geminiChatClient.generateReply("system prompt", history, "current message"))
                .thenReturn("ok");

        client.generateReply("system prompt", history, "current message");

        verify(geminiChatClient).generateReply("system prompt", history, "current message");
    }
}
