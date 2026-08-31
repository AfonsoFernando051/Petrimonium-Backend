package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteConversationUseCaseImplTest {

    @Mock
    private MentorConversationRepositoryPort conversationRepositoryPort;

    @InjectMocks
    private DeleteConversationUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long CONVERSATION_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenOwnedByUser_Deletes() {
        MentorConversation conversation = new MentorConversation(CONVERSATION_ID, EMAIL, "Title", Instant.now(), Instant.now(), "wallet");
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL, "wallet")).thenReturn(Optional.of(conversation));

        useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.WALLET);

        verify(conversationRepositoryPort).delete(CONVERSATION_ID);
    }

    @Test
    void execute_WhenNotOwnedByUser_ThrowsAndDoesNotDelete() {
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL, "wallet")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.WALLET));

        verify(conversationRepositoryPort, never()).delete(CONVERSATION_ID);
    }

    @Test
    void execute_WhenConversationBelongsToADifferentAppContext_ThrowsAndDoesNotDelete() {
        // The conversation exists for this user under WALLET, but the current session is ACADEMY
        // — the repository is queried with "academy" and correctly finds nothing, since a
        // context-scoped conversation is invisible to every other context.
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL, "academy")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.ACADEMY));

        verify(conversationRepositoryPort, never()).delete(CONVERSATION_ID);
    }

    @Test
    void execute_WithNullAppContext_QueriesWithNullClaim() {
        when(conversationRepositoryPort.findByIdAndUser(CONVERSATION_ID, EMAIL, null)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, CONVERSATION_ID, null));
    }
}
