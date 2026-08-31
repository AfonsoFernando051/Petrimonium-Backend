package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.mentor.dto.MentorTurnDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnthropicChatClientTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final AnthropicChatClient client = new AnthropicChatClient(restTemplate);

    @BeforeEach
    void configureApiKey() {
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "model", "claude-opus-5");
        ReflectionTestUtils.setField(client, "baseUrl", "https://api.anthropic.com/v1");
    }

    private static Map<String, Object> successResponse(String text) {
        Map<String, Object> block = Map.of("type", "text", "text", text);
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(block));
        response.put("stop_reason", "end_turn");
        return response;
    }

    @Test
    void generateReply_WithNoApiKeyConfigured_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(client, "apiKey", "");

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hello"));
    }

    @Test
    void generateReply_WithBlankApiKeyConfigured_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(client, "apiKey", "  ");

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hello"));
    }

    @Test
    void generateReply_WithSuccessfulResponse_ReturnsTheReplyText() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(successResponse("Hello, investor!"));

        String reply = client.generateReply("system prompt", List.of(), "hi");

        assertEquals("Hello, investor!", reply);
    }

    @Test
    void generateReply_SkipsThinkingBlocksAndConcatenatesOnlyTextBlocks() {
        Map<String, Object> thinkingBlock = Map.of("type", "thinking", "text", "");
        Map<String, Object> textBlock = Map.of("type", "text", "text", "Diversification matters.");
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(thinkingBlock, textBlock));
        response.put("stop_reason", "end_turn");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        String reply = client.generateReply("system", List.of(), "hi");

        assertEquals("Diversification matters.", reply);
    }

    @Test
    void generateReply_SendsTheApiKeyAndVersionAsHeadersNeverInTheUrl() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(successResponse("reply"));

        client.generateReply("system", List.of(), "hi");

        org.mockito.ArgumentCaptor<String> urlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(urlCaptor.capture(), entityCaptor.capture(), eq(Map.class));

        assertTrue(!urlCaptor.getValue().contains("test-api-key"));
        assertEquals("test-api-key", entityCaptor.getValue().getHeaders().getFirst("x-api-key"));
        assertEquals("2023-06-01", entityCaptor.getValue().getHeaders().getFirst("anthropic-version"));
    }

    @Test
    void generateReply_MapsMentorHistoryRoleToAssistantAndUserRoleToUser() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(successResponse("reply"));

        List<MentorTurnDTO> history = List.of(
                new MentorTurnDTO("mentor", "previous mentor turn"),
                new MentorTurnDTO("user", "previous user turn")
        );

        client.generateReply("system", history, "current message");

        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");

        assertEquals(3, messages.size());
        assertEquals("assistant", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("user", messages.get(2).get("role"));
        assertEquals("system", body.get("system"));
        assertEquals("default", body.get("fallbacks"));
    }

    @Test
    void generateReply_WhenRestClientExceptionOccurs_WrapsItAsIllegalStateException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithNullResponse_ThrowsIllegalStateException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithRefusalStopReason_ThrowsIllegalStateException() {
        Map<String, Object> block = Map.of("type", "text", "text", "declined");
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(block));
        response.put("stop_reason", "refusal");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithEmptyContent_ThrowsIllegalStateException() {
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of());
        response.put("stop_reason", "end_turn");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithMissingContentKey_ThrowsIllegalStateException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new HashMap<>());

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithOnlyThinkingBlocks_ThrowsIllegalStateException() {
        Map<String, Object> thinkingBlock = Map.of("type", "thinking", "text", "");
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(thinkingBlock));
        response.put("stop_reason", "end_turn");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithBlankReplyText_ThrowsIllegalStateException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(successResponse("   "));

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }
}
