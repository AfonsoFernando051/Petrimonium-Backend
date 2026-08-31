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

class GeminiChatClientTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final GeminiChatClient client = new GeminiChatClient(restTemplate);

    @BeforeEach
    void configureApiKey() {
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "model", "gemini-2.0-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");
    }

    private static Map<String, Object> successResponse(String text) {
        Map<String, Object> part = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of(candidate));
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
    void generateReply_SendsTheApiKeyAsAHeaderNeverInTheUrl() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(successResponse("reply"));

        client.generateReply("system", List.of(), "hi");

        org.mockito.ArgumentCaptor<String> urlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(urlCaptor.capture(), entityCaptor.capture(), eq(Map.class));

        assertTrue(!urlCaptor.getValue().contains("test-api-key"));
        assertEquals("test-api-key", entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key"));
    }

    @Test
    void generateReply_MapsMentorHistoryRoleToModelAndUserRoleToUser() {
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
        List<Map<String, Object>> contents = (List<Map<String, Object>>) body.get("contents");

        assertEquals(3, contents.size());
        assertEquals("model", contents.get(0).get("role"));
        assertEquals("user", contents.get(1).get("role"));
        assertEquals("user", contents.get(2).get("role"));
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
    void generateReply_WithEmptyCandidates_ThrowsIllegalStateException() {
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithMissingCandidatesKey_ThrowsIllegalStateException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new HashMap<>());

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithMissingContent_ThrowsIllegalStateException() {
        Map<String, Object> candidate = new HashMap<>();
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of(candidate));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithMissingParts_ThrowsIllegalStateException() {
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of(candidate));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }

    @Test
    void generateReply_WithEmptyParts_ThrowsIllegalStateException() {
        Map<String, Object> content = Map.of("parts", List.of());
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of(candidate));
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

    @Test
    void generateReply_WithNonStringTextField_ThrowsIllegalStateException() {
        Map<String, Object> part = Map.of("text", 42);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> candidate = Map.of("content", content);
        Map<String, Object> response = new HashMap<>();
        response.put("candidates", List.of(candidate));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        assertThrows(IllegalStateException.class,
                () -> client.generateReply("system", List.of(), "hi"));
    }
}
