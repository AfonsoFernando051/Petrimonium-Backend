package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RenameConversationUseCaseImpl implements RenameConversationUseCase {

    private final MentorConversationRepositoryPort conversationRepositoryPort;

    public RenameConversationUseCaseImpl(MentorConversationRepositoryPort conversationRepositoryPort) {
        this.conversationRepositoryPort = conversationRepositoryPort;
    }

    @Override
    public void execute(String email, Long conversationId, String title) {
        conversationRepositoryPort.findByIdAndUser(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        conversationRepositoryPort.updateTitle(conversationId, title);
    }
}
