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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    void execute_PassesTheOwnerAndContextIntoTheWriteItself() {
        useCase.execute(EMAIL, CONVERSATION_ID, "New title", AppContextEnum.WALLET);

        // The ownership check is no longer a separate step the use case performs — it is part of
        // the write's address, so it cannot be skipped (DEM-71).
        verify(conversationRepositoryPort).updateTitle(CONVERSATION_ID, EMAIL, "wallet", "New title");
        verify(conversationRepositoryPort, never()).findByIdAndUser(anyLong(), anyString(), anyString());
    }

    @Test
    void execute_WhenTheScopedWriteFindsNothing_PropagatesNotFound() {
        doThrow(new ResourceNotFoundException("Conversation not found"))
                .when(conversationRepositoryPort).updateTitle(CONVERSATION_ID, EMAIL, "wallet", "New title");

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(EMAIL, CONVERSATION_ID, "New title", AppContextEnum.WALLET));
    }

    @Test
    void execute_CarriesTheAppContextThrough_SoAWalletIdIsNotWritableFromAcademy() {
        useCase.execute(EMAIL, CONVERSATION_ID, "New title", AppContextEnum.ACADEMY);

        verify(conversationRepositoryPort).updateTitle(CONVERSATION_ID, EMAIL, "academy", "New title");
    }

    @Test
    void execute_WithNoAppContext_PassesNullRatherThanGuessing() {
        useCase.execute(EMAIL, CONVERSATION_ID, "New title", null);

        verify(conversationRepositoryPort).updateTitle(CONVERSATION_ID, EMAIL, null, "New title");
    }
}
