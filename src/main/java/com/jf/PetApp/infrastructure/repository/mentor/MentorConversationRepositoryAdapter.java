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
    public MentorConversation create(String userEmail, String title, String appContext) {
        UserJpaEntity user = userJpaRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instant now = Instant.now();
        MentorConversationJpaEntity entity = new MentorConversationJpaEntity();
        entity.setUser(user);
        entity.setTitle(title);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setAppContext(appContext);

        return toDomain(conversationJpaRepository.save(entity), userEmail);
    }

    @Override
    public List<MentorConversation> findAllByUser(String userEmail, String appContext) {
        return conversationJpaRepository.findByUser_EmailAndAppContextOrderByUpdatedAtDesc(userEmail, appContext).stream()
                .map(entity -> toDomain(entity, userEmail))
                .toList();
    }

    @Override
    public Optional<MentorConversation> findByIdAndUser(Long id, String userEmail, String appContext) {
        return conversationJpaRepository.findByIdAndUser_EmailAndAppContext(id, userEmail, appContext)
                .map(entity -> toDomain(entity, userEmail));
    }

    @Override
    @Transactional
    public void updateTitle(Long id, String userEmail, String appContext, String title) {
        MentorConversationJpaEntity entity = ownedOrThrow(id, userEmail, appContext);
        entity.setTitle(title);
        entity.setUpdatedAt(Instant.now());
        conversationJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void touch(Long id, String userEmail, String appContext) {
        MentorConversationJpaEntity entity = ownedOrThrow(id, userEmail, appContext);
        entity.setUpdatedAt(Instant.now());
        conversationJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id, String userEmail, String appContext) {
        conversationJpaRepository.delete(ownedOrThrow(id, userEmail, appContext));
    }

    /**
     * Resolves a conversation only when it belongs to this user AND this app_context. Anything
     * else is reported as missing rather than forbidden, so an id cannot be used to probe for the
     * existence of another user's — or another app's — conversation.
     */
    private MentorConversationJpaEntity ownedOrThrow(Long id, String userEmail, String appContext) {
        return conversationJpaRepository.findByIdAndUser_EmailAndAppContext(id, userEmail, appContext)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private MentorConversation toDomain(MentorConversationJpaEntity entity, String userEmail) {
        return new MentorConversation(
                entity.getId(),
                userEmail,
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAppContext()
        );
    }
}
