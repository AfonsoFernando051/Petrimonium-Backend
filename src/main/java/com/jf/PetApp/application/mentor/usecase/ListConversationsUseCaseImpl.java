package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.mentor.dto.ConversationSummaryDTO;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.core.domain.MentorMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListConversationsUseCaseImpl implements ListConversationsUseCase {

    private final MentorConversationRepositoryPort conversationRepositoryPort;
    private final MentorMessageRepositoryPort messageRepositoryPort;

    public ListConversationsUseCaseImpl(MentorConversationRepositoryPort conversationRepositoryPort,
                                         MentorMessageRepositoryPort messageRepositoryPort) {
        this.conversationRepositoryPort = conversationRepositoryPort;
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public List<ConversationSummaryDTO> execute(String email) {
        List<MentorConversation> conversations = conversationRepositoryPort.findAllByUser(email);

        return conversations.stream()
                .map(conversation -> new ConversationSummaryDTO(
                        conversation.id(),
                        conversation.title(),
                        conversation.updatedAt(),
                        lastMessagePreview(conversation.id())))
                .toList();
    }

    private String lastMessagePreview(Long conversationId) {
        List<MentorMessage> lastMessage = messageRepositoryPort.findRecentByConversation(conversationId, 1);
        return lastMessage.isEmpty() ? null : lastMessage.get(0).text();
    }
}
