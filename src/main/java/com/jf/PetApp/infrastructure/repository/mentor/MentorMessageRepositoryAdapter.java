package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorMessageRepositoryPort;
import com.jf.PetApp.core.domain.MentorMessage;
import com.jf.PetApp.infrastructure.entity.MentorConversationJpaEntity;
import com.jf.PetApp.infrastructure.entity.MentorMessageJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The only place in the codebase that knows Mentor messages are stored as
 * JPA entities. Implements {@link MentorMessageRepositoryPort} so every use
 * case upstream works with the plain {@link MentorMessage} domain record
 * instead.
 */
@Repository
public class MentorMessageRepositoryAdapter implements MentorMessageRepositoryPort {

    private final SpringMentorMessageJpaRepository messageJpaRepository;
    private final SpringMentorConversationJpaRepository conversationJpaRepository;

    public MentorMessageRepositoryAdapter(SpringMentorMessageJpaRepository messageJpaRepository,
                                           SpringMentorConversationJpaRepository conversationJpaRepository) {
        this.messageJpaRepository = messageJpaRepository;
        this.conversationJpaRepository = conversationJpaRepository;
    }

    @Override
    @Transactional
    public MentorMessage append(Long conversationId, String role, String text) {
        MentorConversationJpaEntity conversation = conversationJpaRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        MentorMessageJpaEntity entity = new MentorMessageJpaEntity();
        entity.setConversation(conversation);
        entity.setRole(role);
        entity.setContent(text);
        entity.setCreatedAt(Instant.now());

        return toDomain(messageJpaRepository.save(entity));
    }

    @Override
    public List<MentorMessage> findAllByConversation(Long conversationId) {
        return messageJpaRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MentorMessage> findRecentByConversation(Long conversationId, int limit) {
        List<MentorMessageJpaEntity> mostRecentFirst = new ArrayList<>(
                messageJpaRepository.findByConversation_IdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, limit)));
        Collections.reverse(mostRecentFirst);
        return mostRecentFirst.stream().map(this::toDomain).toList();
    }

    private MentorMessage toDomain(MentorMessageJpaEntity entity) {
        return new MentorMessage(
                entity.getId(),
                entity.getConversation().getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
