package com.jf.PetApp.infrastructure.repository.mentor;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.mentor.port.MentorConversationRepositoryPort;
import com.jf.PetApp.core.domain.MentorConversation;
import com.jf.PetApp.infrastructure.entity.MentorConversationJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The only place in the codebase that knows Mentor conversations are stored
 * as JPA entities. Implements {@link MentorConversationRepositoryPort} so
 * every use case upstream works with the plain {@link MentorConversation}
 * domain record instead.
 */
@Repository
public class MentorConversationRepositoryAdapter implements MentorConversationRepositoryPort {

    private final SpringMentorConversationJpaRepository conversationJpaRepository;
    private final SpringUserJpaRepository userJpaRepository;

    public MentorConversationRepositoryAdapter(SpringMentorConversationJpaRepository conversationJpaRepository,
                                                 SpringUserJpaRepository userJpaRepository) {
        this.conversationJpaRepository = conversationJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public MentorConversation create(String userEmail, String title) {
        UserJpaEntity user = userJpaRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instant now = Instant.now();
        MentorConversationJpaEntity entity = new MentorConversationJpaEntity();
        entity.setUser(user);
        entity.setTitle(title);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return toDomain(conversationJpaRepository.save(entity), userEmail);
    }

    @Override
    public List<MentorConversation> findAllByUser(String userEmail) {
        return conversationJpaRepository.findByUser_EmailOrderByUpdatedAtDesc(userEmail).stream()
                .map(entity -> toDomain(entity, userEmail))
                .toList();
    }

    @Override
    public Optional<MentorConversation> findByIdAndUser(Long id, String userEmail) {
        return conversationJpaRepository.findByIdAndUser_Email(id, userEmail)
                .map(entity -> toDomain(entity, userEmail));
    }

    @Override
    @Transactional
    public void updateTitle(Long id, String title) {
        MentorConversationJpaEntity entity = conversationJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        entity.setTitle(title);
        entity.setUpdatedAt(Instant.now());
        conversationJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void touch(Long id) {
        MentorConversationJpaEntity entity = conversationJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        entity.setUpdatedAt(Instant.now());
        conversationJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        conversationJpaRepository.deleteById(id);
    }

    private MentorConversation toDomain(MentorConversationJpaEntity entity, String userEmail) {
        return new MentorConversation(
                entity.getId(),
                userEmail,
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
