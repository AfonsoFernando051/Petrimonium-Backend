package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
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
        conversationRepositoryPort.findByIdAndUser(conversationId, email, appContext == null ? null : appContext.claimValue())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        conversationRepositoryPort.delete(conversationId);
    }
}
