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

    /**
     * @param appContext the {@code AppContextEnum.claimValue()} the conversation belongs to
     *                    ({@code null} only for a session with no resolvable app_context, which
     *                    {@code SecurityConfig} already rejects for {@code /api/mentor/**} —
     *                    kept nullable here purely to match {@link MentorConversation#appContext()}).
     */
    MentorConversation create(String userEmail, String title, String appContext);

    /** Only conversations created under {@code appContext} — never another context's threads. */
    List<MentorConversation> findAllByUser(String userEmail, String appContext);

    /** {@code Optional.empty()} if the conversation belongs to a different app_context, exactly
     *  as if it didn't exist for this user — a Wallet session can't even probe for an Academy
     *  conversation's existence by id. */
    Optional<MentorConversation> findByIdAndUser(Long id, String userEmail, String appContext);

    void updateTitle(Long id, String title);

    /** Bumps {@code updatedAt} to now, without touching the title. */
    void touch(Long id);

    void delete(Long id);
}
