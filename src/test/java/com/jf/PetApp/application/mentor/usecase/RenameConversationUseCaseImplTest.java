package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RenameConversationUseCaseImplTest {

    @Mock
    private MentorConversationRepositoryPort conversationRepositoryPort;

    @InjectMocks
    private RenameConversationUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long CONVERSATION_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenOwnedByUser_UpdatesTitle() {
        MentorConversation conversation = new MentorConversation(CONVERSATION_ID, EMAIL, "Old title", Instant.now(), Instant.now());
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL)).thenReturn(Optional.of(conversation));

        useCase.execute(EMAIL, CONVERSATION_ID, "New title");

        verify(conversationRepositoryPort).updateTitle(CONVERSATION_ID, "New title");
    }

    @Test
    void execute_WhenNotOwnedByUser_ThrowsAndDoesNotUpdate() {
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, CONVERSATION_ID, "New title"));
    }
}
