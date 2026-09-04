package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.springframework.stereotype.Service;

@Service
public class DeleteConversationUseCaseImpl implements DeleteConversationUseCase {

    private final MentorConversationRepositoryPort conversationRepositoryPort;

    public DeleteConversationUseCaseImpl(MentorConversationRepositoryPort conversationRepositoryPort) {
        this.conversationRepositoryPort = conversationRepositoryPort;
    }

    @Override
    public void execute(String email, Long conversationId, AppContextEnum appContext) {
        // No separate ownership pre-check: the scoped delete below finds nothing (and throws the
        // same ResourceNotFoundException) when the conversation isn't this user's in this context.
        conversationRepositoryPort.delete(
                conversationId, email, appContext == null ? null : appContext.claimValue());
    }
}
