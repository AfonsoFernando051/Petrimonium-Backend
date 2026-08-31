package com.jf.PetApp.application.mentor.port;

import com.jf.PetApp.core.domain.MentorConversation;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer boundary for Mentor conversation persistence. Use cases
 * depend on this port, never on Spring Data or JPA entities directly — the
 * adapter in {@code infrastructure.repository.mentor} is the only place that
 * knows how conversations are actually stored.
 */
public interface MentorConversationRepositoryPort {

    MentorConversation create(String userEmail, String title);

    List<MentorConversation> findAllByUser(String userEmail);

    Optional<MentorConversation> findByIdAndUser(Long id, String userEmail);

    void updateTitle(Long id, String title);

    /** Bumps {@code updatedAt} to now, without touching the title. */
    void touch(Long id);

    void delete(Long id);
}
