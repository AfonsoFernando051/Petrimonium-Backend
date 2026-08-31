package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.dto.ConversationDetailDTO;
import com.jf.PetApp.application.mentor.dto.MentorMessageDTO;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import org.springframework.stereotype.Service;

@Service
public class GetConversationUseCaseImpl implements GetConversationUseCase {

    private final MentorConversationRepositoryPort conversationRepositoryPort;
    private final MentorMessageRepositoryPort messageRepositoryPort;

    public GetConversationUseCaseImpl(MentorConversationRepositoryPort conversationRepositoryPort,
                                       MentorMessageRepositoryPort messageRepositoryPort) {
        this.conversationRepositoryPort = conversationRepositoryPort;
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public ConversationDetailDTO execute(String email, Long conversationId) {
        MentorConversation conversation = conversationRepositoryPort.findByIdAndUser(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        var messages = messageRepositoryPort.findAllByConversation(conversationId).stream()
                .map(m -> new MentorMessageDTO(m.id(), m.role(), m.text(), m.createdAt()))
                .toList();

        return new ConversationDetailDTO(conversation.id(), conversation.title(), messages);
    }
}
