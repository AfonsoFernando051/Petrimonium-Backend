package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
    void execute_PassesTheOwnerAndContextIntoTheDeleteItself() {
        useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.WALLET);

        // The ownership check is no longer a separate step the use case performs — it is part of
        // the delete's address, so it cannot be skipped (DEM-71).
        verify(conversationRepositoryPort).delete(CONVERSATION_ID, EMAIL, "wallet");
        verify(conversationRepositoryPort, never()).findByIdAndUser(anyLong(), anyString(), anyString());
    }

    @Test
    void execute_WhenTheScopedDeleteFindsNothing_PropagatesNotFound() {
        doThrow(new ResourceNotFoundException("Conversation not found"))
                .when(conversationRepositoryPort).delete(CONVERSATION_ID, EMAIL, "wallet");

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.WALLET));
    }

    @Test
    void execute_CarriesTheAppContextThrough_SoAWalletIdIsNotDeletableFromAcademy() {
        useCase.execute(EMAIL, CONVERSATION_ID, AppContextEnum.ACADEMY);

        verify(conversationRepositoryPort).delete(CONVERSATION_ID, EMAIL, "academy");
    }

    @Test
    void execute_WithNullAppContext_PassesNullRatherThanGuessing() {
        useCase.execute(EMAIL, CONVERSATION_ID, null);

        verify(conversationRepositoryPort).delete(CONVERSATION_ID, EMAIL, null);
    }
}
