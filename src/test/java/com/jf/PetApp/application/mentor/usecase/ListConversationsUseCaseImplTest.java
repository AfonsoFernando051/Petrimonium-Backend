package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.MentorMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ListConversationsUseCaseImplTest {

    @Mock
    private MentorConversationRepositoryPort conversationRepositoryPort;
    @Mock
    private MentorMessageRepositoryPort messageRepositoryPort;

    @InjectMocks
    private ListConversationsUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ReturnsSummariesWithLastMessagePreview() {
        MentorConversation conversation = new MentorConversation(1L, EMAIL, "Dividends 101", Instant.now(), Instant.now());
        when(conversationRepositoryPort.findAllByUser(EMAIL)).thenReturn(List.of(conversation));
        when(messageRepositoryPort.findRecentByConversation(1L, 1))
                .thenReturn(List.of(new MentorMessage(5L, 1L, "mentor", "Dividends are periodic payments...", Instant.now())));

        List<ConversationSummaryDTO> result = useCase.execute(EMAIL);

        assertEquals(1, result.size());
        assertEquals("Dividends 101", result.get(0).title());
        assertEquals("Dividends are periodic payments...", result.get(0).lastMessagePreview());
    }

    @Test
    void execute_WhenConversationHasNoMessagesYet_PreviewIsNull() {
        MentorConversation conversation = new MentorConversation(1L, EMAIL, null, Instant.now(), Instant.now());
        when(conversationRepositoryPort.findAllByUser(EMAIL)).thenReturn(List.of(conversation));
        when(messageRepositoryPort.findRecentByConversation(1L, 1)).thenReturn(List.of());

        List<ConversationSummaryDTO> result = useCase.execute(EMAIL);

        assertEquals(1, result.size());
        assertEquals(null, result.get(0).lastMessagePreview());
    }
}
