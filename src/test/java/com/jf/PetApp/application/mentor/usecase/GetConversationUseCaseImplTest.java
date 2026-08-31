package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class GetConversationUseCaseImplTest {

    @Mock
    private MentorConversationRepositoryPort conversationRepositoryPort;
    @Mock
    private MentorMessageRepositoryPort messageRepositoryPort;

    @InjectMocks
    private GetConversationUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long CONVERSATION_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenOwnedByUser_ReturnsMessagesInOrder() {
        MentorConversation conversation = new MentorConversation(CONVERSATION_ID, EMAIL, "Dividends 101", Instant.now(), Instant.now());
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL)).thenReturn(Optional.of(conversation));
        when(messageRepositoryPort.findAllByConversation(CONVERSATION_ID)).thenReturn(List.of(
                new MentorMessage(1L, CONVERSATION_ID, "user", "What are dividends?", Instant.now()),
                new MentorMessage(2L, CONVERSATION_ID, "mentor", "Dividends are periodic payments...", Instant.now())));

        ConversationDetailDTO result = useCase.execute(EMAIL, CONVERSATION_ID);

        assertEquals("Dividends 101", result.title());
        assertEquals(2, result.messages().size());
        assertEquals("What are dividends?", result.messages().get(0).text());
    }

    @Test
    void execute_WhenNotOwnedByUser_ThrowsNotFound() {
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, CONVERSATION_ID));
    }
}
